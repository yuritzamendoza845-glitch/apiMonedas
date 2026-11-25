# apiMonedas — Evaluacion 5: Control de Concurrencia

Laboratorio de deteccion y mitigacion de condiciones de carrera en contadores
compartidos, sobre el punto de instrumentacion `UsuarioServicio.login(...)`.

## Estructura del proyecto (Maven multi-modulo)

```
apiMonedas/
├── dominio/                 Entidades de dominio (Usuario)
├── aplicacion/               Capa de aplicacion
│   └── servicios/
│       ├── IContadorIntentosLogin.java   Contrato comun de las 3 variantes
│       ├── ContadorSinControl.java       Variante A (sin control de concurrencia)
│       ├── ContadorSincronizado.java     Variante B (synchronized)
│       ├── ContadorAtomico.java          Variante C (AtomicLong)
│       └── UsuarioServicio.java          login(...) instrumentado con el contador inyectado
└── presentacion/              Capa de presentacion (HTTP/Tomcat en el sistema real)
    ├── controlador/
    │   └── MonedaControlador.java
    └── src/test/java/.../PruebaConcurrenciaContadores.java   Prueba de concurrencia (JUnit 5)
```

## Requisitos

- JDK 17
- Maven 3.9+

## Como correrlo

```bash
mvn clean test
```

Esto compila los 3 modulos y ejecuta `PruebaConcurrenciaContadores`, que dispara
rafagas de 500, 1000 y 2000 hilos concurrentes contra cada una de las 3
variantes del contador. Cada hilo ejecuta 5.000 intentos de `login(...)`
(constante `INTENTOS_POR_HILO`), de modo que el total esperado es
`hilos x 5000`. Se usa `ExecutorService` + `CountDownLatch` (patron de
"puerta": todos los hilos se liberan al mismo tiempo para maximizar la
concurrencia real al momento de medir).

Al terminar, imprime la tabla comparativa en consola y la guarda en
`presentacion/resultados-concurrencia.csv`.

Las variantes B y C tienen asserts (`assertEquals`) que fallan la build si se
pierde algun incremento; la variante A no se afirma a proposito, porque su
comportamiento con perdidas es el fenomeno que el laboratorio pide evidenciar.

> Por que 5.000 intentos por hilo: con un solo incremento por hilo la ventana
> de contencion es de nanosegundos y la carrera casi nunca se manifiesta (en
> una primera version de la prueba, la Variante A salio con 0 perdidos).
> Repetir el incremento dentro de cada hilo alarga esa ventana y hace visible
> el fenomeno.

## Resultados obtenidos (corrida con JDK 17 Temurin)

| Variante           | Hilos | Esperado   | Obtenido   | Perdidos  | Tiempo (ms) |
|--------------------|------:|-----------:|-----------:|----------:|------------:|
| A - Sin control    |   500 |  2.500.000 |    851.223 | 1.648.777 |         294 |
| A - Sin control    |  1000 |  5.000.000 |  2.238.337 | 2.761.663 |         128 |
| A - Sin control    |  2000 | 10.000.000 |  3.987.283 | 6.012.717 |         241 |
| B - synchronized   |   500 |  2.500.000 |  2.500.000 |         0 |         278 |
| B - synchronized   |  1000 |  5.000.000 |  5.000.000 |         0 |         545 |
| B - synchronized   |  2000 | 10.000.000 | 10.000.000 |         0 |         921 |
| C - AtomicLong     |   500 |  2.500.000 |  2.500.000 |         0 |         107 |
| C - AtomicLong     |  1000 |  5.000.000 |  5.000.000 |         0 |         173 |
| C - AtomicLong     |  2000 | 10.000.000 | 10.000.000 |         0 |         214 |

> En una corrida anterior la Variante A perdio entre el 53 % y el 83 % de los
> incrementos, y en esta entre el 55 % y el 66 %: los valores cambian en cada
> ejecucion.
>
> Nota: los valores de la Variante A cambian en cada ejecucion, porque una
> condicion de carrera depende de como el planificador de hilos entrelaza las
> operaciones en ese momento. Las Variantes B y C dieron 0 perdidas siempre.
> Los tiempos dependen del hardware.

## Analisis comparativo

### Por que la Variante A pierde incrementos

`contador++` no es una operacion atomica. Se ejecuta en tres pasos
independientes:

1. **Leer** el valor actual de `contador`.
2. **Sumarle 1** en un registro de CPU.
3. **Escribir** el resultado de vuelta en `contador`.

Si el hilo 1 lee 417 y, antes de escribir 418, el hilo 2 tambien lee 417 y
escribe 418, uno de los dos incrementos se pierde: ambos creian sumar desde
417, pero solo uno "sobrevive" (*lost update*). Ademas, sin sincronizacion ni
`volatile` no hay garantia de visibilidad entre hilos. En la medicion se
perdio entre el 55 % y el 66 % de los incrementos (66 %, 55 % y 60 % para 500,
1000 y 2000 hilos), con porcentajes que no crecen de forma ordenada con el
numero de hilos y que cambian entre corridas, lo que confirma que el
resultado es no determinista.

### Variante B (`synchronized`) vs Variante C (`AtomicLong`)

| Criterio | B - synchronized | C - AtomicLong |
|---|---|---|
| **Correccion** | Correcta siempre (0 perdidas) | Correcta siempre (0 perdidas) |
| **Mecanismo** | Exclusion mutua con monitor: un hilo entra, los demas se **bloquean** | Lock-free con CAS (Compare-And-Swap) en hardware: ningun hilo se bloquea, el que pierde reintenta |
| **Tiempo medido** | 278 / 545 / 921 ms | 107 / 173 / 214 ms |
| **Cuando conviene** | Cuando la seccion critica involucra varias operaciones o variables que deben ser consistentes en conjunto | Cuando la operacion es unica y simple (incrementar, leer, comparar-e-intercambiar), como contar intentos de login; mismo enfoque que Cubeta de Tokens |

En esta medicion `AtomicLong` fue entre 2,6 y 4,3 veces mas rapida que
`synchronized`, por evitar el bloqueo y los cambios de contexto. La diferencia
exacta depende del hardware y de la carga. El tiempo de la Variante A no es
comparable: es el de un resultado incorrecto.

### Conclusion tecnica

Un contador compartido sin control de concurrencia (Variante A) no es seguro:
bajo rafagas concurrentes subregistra intentos de login reales, lo que en un
sistema de seguridad (deteccion de fuerza bruta, bloqueo de cuentas) significa
fallar silenciosamente justo en el escenario que mas importa proteger. Tanto
`synchronized` como `AtomicLong` resuelven la correccion por completo. Para un
contador simple como este, `AtomicLong` es preferible: misma garantia de
correccion con menor costo bajo alta concurrencia. `synchronized` sigue siendo
la herramienta correcta cuando la seccion critica es mas compleja que una sola
variable atomica.
