package org.cyphail.ast;

/** Un elemento de SET: asignar una propiedad o agregar etiquetas. */
public sealed interface SetItem permits SetProperty, SetLabels {
}