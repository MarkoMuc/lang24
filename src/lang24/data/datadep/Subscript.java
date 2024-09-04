package lang24.data.datadep;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;

import java.util.Vector;
import java.util.stream.Collectors;

public class Subscript {
    public Vector<Term> terms = new Vector<>();

    public void addTerm(AstDefn variable, Integer coef) {
        terms.add(new Term(variable, coef));
    }

    public void addTerm(AstDefn variable, AstAtomExpr coef) {
        terms.add(new Term(variable, coef));
    }

    public void addTerm(Integer coef) {
        terms.add(new Term(coef));
    }

    public void addTerm(AstAtomExpr coef) {
        terms.add(new Term(coef));
    }

    public void addTerm(AstDefn variable) {
        terms.add(new Term(variable));
    }

    public void collect() {
        Vector<Term> tmp = terms.stream()
                .filter(x -> x.variable == null)
                .collect(Collectors.toCollection(Vector::new));
        if (tmp.size() > 1) {
            Term last = new Term(
                    tmp.stream()
                            .map(t -> t.coefficient)
                            .reduce(0, Integer::sum));
            terms.removeAll(tmp);
            terms.add(last);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(terms.stream()
                .filter(x -> x.variable != null)
                .map(Term::toString)
                .collect(Collectors.joining("+")));

        sb.append("+").append(terms.stream()
                .filter(x -> x.variable == null)
                .map(Term::toString)
                .collect(Collectors.joining("+")));
        if (sb.lastIndexOf("+") == sb.length() - 1) {
            sb.replace(sb.length() - 1, sb.length(), "");
        }
        return sb.toString();
    }

    public class Term {
        public AstDefn variable;
        public Integer coefficient;

        public Term(AstDefn variable, Integer coefficient) {
            this.variable = variable;
            this.coefficient = coefficient;
        }

        public Term(AstDefn variable) {
            this.variable = variable;
            this.coefficient = 1;
        }

        public Term(Integer coefficient) {
            this.variable = null;
            this.coefficient = coefficient;
        }

        public Term(AstAtomExpr coef) {
            if (coef.type != AstAtomExpr.Type.INT) {
                throw new Report.InternalError();
            }
            this.variable = null;
            this.coefficient = Integer.parseInt(coef.toString());
        }

        public Term(AstDefn variable, AstAtomExpr coef) {
            if (coef.type != AstAtomExpr.Type.INT) {
                throw new Report.InternalError();
            }
            this.variable = variable;
            this.coefficient = Integer.parseInt(coef.toString());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (this.coefficient > 1 && this.variable == null) {
                sb.append(coefficient);
            }

            if (this.variable != null) {
                sb.append(variable.name);
            }

            return sb.toString();
        }
    }
}
