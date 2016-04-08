package dynapiclient.utils

import org.codehaus.groovy.runtime.*


class AutocompleteMetaClass extends DelegatingMetaClass {
    final MetaClass originalMetaClass

    static void addFakeMethodsToObject(Object object, Collection<String> methods, Collection<String> properties) {
        def autocomplete = configureAutocompleteMetaClassInObject(object)
        def innerMeta = autocomplete.originalMetaClass
        addFakeMethodsToExpando(innerMeta, object, methods, properties)
    }

    static AutocompleteMetaClass configureAutocompleteMetaClassInObject(Object object) {
        if (!hasAutocompleteMeta(object)) {
            object.getMetaClass().help = { ...args ->
                object.methodMissing(innerMeta, args)
            }
        }

        return configureMetaClass(object)
    }

    private static void addFakeMethodsToExpando(MetaClass meta, Object object, Collection<String> methods, Collection<String> properties) {
        for (def method : methods) {
            meta."${method}" = { ...args ->
                object.methodMissing(innerMeta, args)
            }
        }

        // TODO Properties Not yet working
    }

    private static boolean hasAutocompleteMeta(Object object) {
        def metaOld = InvokerHelper.getMetaClass(object)
        return (metaOld.getClass().name == AutocompleteMetaClass.class.name)
    }

    private static MetaClass configureMetaClass(Object object) {
        def metaOld = InvokerHelper.getMetaClass(object)
        if (metaOld.getClass().name != AutocompleteMetaClass.class.name) {
            object.metaClass = new AutocompleteMetaClass(metaOld)
        }
        return InvokerHelper.getMetaClass(object)
    }

    AutocompleteMetaClass(MetaClass originalMetaClass) {
        super(originalMetaClass)
        this.originalMetaClass = originalMetaClass
    }

    List<MetaMethod> getMetaMethods() {
        return originalMetaClass.getExpandoMethods()
    }

    List<MetaBeanProperty> getProperties() {
        return originalMetaClass.getExpandoProperties()
    }
}
