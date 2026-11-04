package org.cyphail.ast;

/*
 * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 */

/**
 * Una expresion tal como se escribe en Cyphail: a.name, a, 18, a.age > 18, NOT ...
 * Es el nombre de columna de un RETURN sin alias y el texto de los mensajes de error.
 * Unico lugar donde se arma ese texto (lo usan VariableValidator y CodeGen).
 * AstPrinter, en cambio, imprime el arbol en notacion prefija: (> (. a age) 18).
 */
public final class ExprText {

    private ExprText() {}

    public static String text(Expr expr) {
        return switch (expr) {
            case PropertyAccess(String variable, String property) -> variable + "." + property;
            case Var(String name) -> name;
            case NumberLit(long value) -> Long.toString(value);
            case DecimalLit(double value) -> Double.toString(value);
            case StringLit(String value) -> value;
            case BoolLit(boolean value) -> Boolean.toString(value);
            case Binary(BinaryOp op, Expr left, Expr right) -> text(left) + " " + op.symbol() + " " + text(right);
            case And(Expr left, Expr right) -> text(left) + " AND " + text(right);
            case Or(Expr left, Expr right) -> text(left) + " OR " + text(right);
            case Not(Expr operand) -> "NOT " + text(operand);
        };
    }
}
