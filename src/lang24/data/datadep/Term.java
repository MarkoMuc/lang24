package lang24.data.datadep;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstPfxExpr;

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

    public Term(AstPfxExpr coef) {
        if (coef.expr instanceof AstAtomExpr atom) {
            if (atom.type != AstAtomExpr.Type.INT) {
                throw new Report.InternalError();
            }
            this.variable = null;

            if (coef.oper == AstPfxExpr.Oper.ADD) {
                this.coefficient = Integer.parseInt(atom.value);
            } else if (coef.oper == AstPfxExpr.Oper.SUB) {
                this.coefficient = -Integer.parseInt(atom.value);
            } else {
                throw new Report.InternalError();
            }

        }
    }

    public Term(AstDefn variable, AstAtomExpr coef) {
        if (coef.type != AstAtomExpr.Type.INT) {
            throw new Report.InternalError();
        }
        this.variable = variable;
        this.coefficient = Integer.parseInt(coef.toString());
    }

    public Term(Term t1, Term t2) {
        this.variable = t1.variable == null ? t2.variable : t1.variable;
        this.coefficient = t1.coefficient * t2.coefficient;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (this.variable != null) {
            if (this.coefficient != 1) {
                if (this.coefficient < 0) {
                    sb.append('(').append(this.coefficient).append(")");
                } else {
                    sb.append(this.coefficient);
                }

            }
            sb.append(this.variable.name);
        } else {
            sb.append(this.coefficient);
        }

        return sb.toString();
    }
}
