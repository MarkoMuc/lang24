package lang24.data.datadep.subscript;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.expr.AstPfxExpr;
import lang24.data.datadep.LoopDescriptor;

/**
 * @author marko.muc12@gmail.com
 */
public class Term {
    public AstDefn variable;
    public AstExpr name;
    public LoopDescriptor loop;
    public Integer coefficient;
    public Integer depth;

    public Term(AstDefn variable, AstExpr name, Integer coefficient, Integer depth) {
        this.variable = variable;
        this.name = name;
        this.depth = depth;
        this.coefficient = coefficient;
    }

    public Term(AstDefn variable, AstExpr name, LoopDescriptor loop) {
        this.variable = variable;
        this.name = name;
        this.loop = loop;
        this.depth = loop.getDepth();
        this.coefficient = 1;
    }

    public Term(Integer coefficient) {
        this.variable = null;
        this.depth = null;
        this.coefficient = coefficient;
    }

    public Term(AstAtomExpr coef) {
        if (coef.type != AstAtomExpr.Type.INT) {
            throw new Report.InternalError();
        }
        this.variable = null;
        this.depth = null;
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
            this.depth = null;
        }
    }

    public Term(AstPfxExpr coef, AstDefn variable, AstExpr name, Integer depth) {
        this.variable = variable;
        this.name = name;
        this.depth = depth;
        if (coef.oper == AstPfxExpr.Oper.ADD) {
            this.coefficient = 1;
        } else if (coef.oper == AstPfxExpr.Oper.SUB) {
            this.coefficient = -1;
        } else {
            throw new Report.InternalError();
        }
    }

    public Term(Term t1, Term t2) {
        this.variable = t1.variable == null ? t2.variable : t1.variable;
        this.name = t1.name == null ? t2.name : t1.name;
        this.depth = t1.variable == null ? t2.depth : t1.depth;
        this.coefficient = t1.coefficient * t2.coefficient;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (this.variable != null) {
            if (this.coefficient != 1) {
                if (this.coefficient < 0) {
                    sb.append('(').append(this.coefficient).append(')');
                } else {
                    sb.append(this.coefficient);
                }

            }
            sb.append(this.variable.name);
            sb.append('[').append(this.depth).append(']');
        } else {
            sb.append(this.coefficient);
        }

        return sb.toString();
    }
}
