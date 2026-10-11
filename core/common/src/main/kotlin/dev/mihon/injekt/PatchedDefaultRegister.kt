package dev.mihon.injekt

import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.TypeReference
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.registry.default.DefaultRegistrar
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * Stock [DefaultRegistrar] with one fix (from mihonapp/injekt, itself
 * inorichi/injekt@65b0440): [ConcurrentHashMap.getOrPut] may invoke its
 * default more than once under contention, so singleton factories get wrapped
 * in [lazy] to guarantee single evaluation. Everything else delegates, so
 * this stays compatible with the injekt-core API version in use.
 */
internal class PatchedDefaultRegister(
    private val delegate: InjektRegistrar = DefaultRegistrar(),
) : InjektRegistrar by delegate {

    private data class Instance(val forWhatType: Type, val forKey: Any)

    private object NOKEY

    private val existingValues = ConcurrentHashMap<Instance, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any> addSingletonFactory(forType: TypeReference<R>, factoryCalledOnce: () -> R) {
        delegate.addSingletonFactory(forType) {
            val lazy = existingValues.getOrPut(Instance(forType.type, NOKEY)) {
                lazy { factoryCalledOnce() }
            }
            (lazy as Lazy<R>).value
        }
    }
}
