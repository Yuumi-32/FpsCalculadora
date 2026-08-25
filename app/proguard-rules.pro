# kotlinx.serialization gera um `serializer()`/`$serializer` por classe
# @Serializable em com.fps.calculadora.core (Model.kt) — sem isso o R8 pode
# derrubar como código morto, e o JSON da base de dados para de decodificar.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.fps.calculadora.core.**$$serializer { *; }
-keepclassmembers class com.fps.calculadora.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.fps.calculadora.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
