import dsl.Action2
import dsl.compile
import dsl.freeTest

fun main() {

    val result = freeTest.compile {
        when(it) {
            is Action2.Put -> Unit
            is Action2.Read -> "Hello World!"
        }
    }
}
