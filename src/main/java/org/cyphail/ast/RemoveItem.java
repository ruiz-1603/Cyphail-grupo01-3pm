package org.cyphail.ast;

/** Un elemento de REMOVE: quitar una propiedad o quitar etiquetas. */
public sealed interface RemoveItem permits RemoveProperty, RemoveLabels {
}