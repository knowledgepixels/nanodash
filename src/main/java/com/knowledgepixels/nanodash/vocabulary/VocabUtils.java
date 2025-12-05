package com.knowledgepixels.nanodash.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.base.InternedIRI;

// TODO remove this class and use the one defined in nanopub-java when available
public class VocabUtils {

    private VocabUtils() {
    }

    static Namespace createNamespace(String prefix, String namespace) {
        return new VocabularyNamespace(prefix, namespace);
    }

    static IRI createIRI(String namespace, String localName) {
        checkParameter(namespace, "Namespace");
        checkParameter(localName, "Local Name");
        return new InternedIRI(namespace, localName);
    }

    private static class VocabularyNamespace extends AbstractNamespace {

        private final String prefix;
        private final String namespace;

        /**
         * Constructs a new VocabularyNamespace with the given prefix and namespace.
         *
         * @param prefix    the prefix of the namespace
         * @param namespace the full namespace URI
         */
        public VocabularyNamespace(String prefix, String namespace) {
            checkParameter(prefix, "Prefix");
            checkParameter(namespace, "Namespace");
            this.prefix = prefix;
            this.namespace = namespace;
        }

        /**
         * <@inheritDoc>
         */
        @Override
        public String getPrefix() {
            return this.prefix;
        }

        /**
         * <@inheritDoc>
         */
        @Override
        public String getName() {
            return this.namespace;
        }

    }

    static void checkParameter(String parameter, String parameterName) {
        if (parameter == null || parameter.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or empty");
        }
    }

}
