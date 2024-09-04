package lang24.phase.vecan;

import lang24.common.report.Report;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstBinExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.expr.AstPfxExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.datadep.Subscript;
import lang24.data.datadep.Term;
import lang24.phase.seman.SemAn;

//FIXME: add neg numbers
public class SubscriptAnalyzer implements AstFullVisitor<Term, Subscript> {

    private Term previous1;
    private Term previous2;
    private AstBinExpr currStmt = null;

    @Override
    public Term visit(AstBinExpr binExpr, Subscript arg) {
        AstBinExpr prevExpr = currStmt;
        currStmt = binExpr;

        Term fstTerm = binExpr.fstExpr.accept(this, arg);
        Term sndTerm = binExpr.sndExpr.accept(this, arg);

        currStmt = binExpr;

        if (!(binExpr.oper == AstBinExpr.Oper.ADD
                || binExpr.oper == AstBinExpr.Oper.MUL
                || binExpr.oper == AstBinExpr.Oper.SUB)) {
            throw new Report.Error(binExpr, "Illegal operand");
        }

        if (fstTerm == null && sndTerm == null) {
            if (binExpr.oper == AstBinExpr.Oper.ADD) {
                return null;
            } else {
                //IS this true?
                throw new Report.Error(binExpr, "Ilegal operation multiplied");
            }
        } else if (fstTerm != null && sndTerm != null) {
            if (binExpr.oper == AstBinExpr.Oper.ADD || binExpr.oper == AstBinExpr.Oper.SUB) {
                if (fstTerm.variable == null && sndTerm.variable == null) {
                    // CONST + CONST
                    if (binExpr.oper == AstBinExpr.Oper.ADD) {
                        return new Term(fstTerm.coefficient + sndTerm.coefficient);
                    } else {
                        return new Term(fstTerm.coefficient - sndTerm.coefficient);
                    }
                }
                // CONST + VAR
                // VAR + CONST
                // VAR + VAR
                if (prevExpr == null || prevExpr.oper != AstBinExpr.Oper.MUL) {
                    arg.addTerm(fstTerm);
                    arg.addTerm(sndTerm);
                }

                if (binExpr.oper == AstBinExpr.Oper.SUB) {
                    sndTerm.coefficient = -sndTerm.coefficient;
                }

                previous1 = fstTerm;
                previous2 = sndTerm;

                return null;
            } else {
                if (fstTerm.variable == null && sndTerm.variable == null) {
                    // CONST * CONST
                    return new Term(fstTerm.coefficient * sndTerm.coefficient);
                } else if (fstTerm.variable != null && sndTerm.variable != null) {
                    // VAR * VAR
                    throw new Report.Error(binExpr, "Two vars multiplied");
                } else {
                    // CONST * VAR
                    // VAR * CONST
                    return new Term(fstTerm, sndTerm);
                }
            }
        } else if (fstTerm != null) {
            // MUL/CONST/VAR/ADD_C and ADD
            if (binExpr.oper == AstBinExpr.Oper.ADD || binExpr.oper == AstBinExpr.Oper.SUB) {
                if (binExpr.oper == AstBinExpr.Oper.SUB) {
                    previous1.coefficient = -previous1.coefficient;
                    previous2.coefficient = -previous2.coefficient;
                }
                arg.addTerm(fstTerm);
                previous1 = fstTerm;
                previous2 = null;
                return null;
            } else {
                if (fstTerm.variable != null) {
                    throw new Report.Error(binExpr, "Left side is var meaning and right side has var too and we cant multiply");
                }

                if (previous1 != null) {
                    arg.terms.remove(previous1);
                    //arg.removeTermHash(previous1);
                    arg.addTerm(new Term(previous1.variable, previous1.coefficient * fstTerm.coefficient));
                }

                if (previous2 != null) {
                    arg.terms.remove(previous2);
                    //arg.removeTermHash(previous2);
                    arg.addTerm(new Term(previous2.variable, previous2.coefficient * fstTerm.coefficient));
                }
                previous1 = null;
                previous2 = null;

            }
        } else {
            // ADD and MUL/CONST/VAR/ADD_C
            if (binExpr.oper == AstBinExpr.Oper.ADD || binExpr.oper == AstBinExpr.Oper.SUB) {
                if (binExpr.oper == AstBinExpr.Oper.SUB) {
                    sndTerm.coefficient = -sndTerm.coefficient;
                }
                arg.addTerm(sndTerm);
                previous1 = null;
                previous2 = sndTerm;
                return null;
            } else {
                if (sndTerm.variable != null) {
                    throw new Report.Error(binExpr, "Right side is var meaning and left side has var too and we cant multiply");
                }

                if (previous1 != null) {
                    arg.terms.remove(previous1);
                    //arg.removeTermHash(previous1);
                    Term a = new Term(previous1.variable, previous1.coefficient * sndTerm.coefficient);
                    arg.addTerm(a);
                }

                if (previous2 != null) {
                    arg.terms.remove(previous2);
                    //arg.removeTermHash(previous2);
                    Term a = new Term(previous2.variable, previous2.coefficient * sndTerm.coefficient);
                    arg.addTerm(a);
                }

                previous1 = null;
                previous2 = null;
            }
        }

        return null;
    }

    @Override
    public Term visit(AstAtomExpr atomExpr, Subscript arg) {
        Term num = new Term(atomExpr);
        if (currStmt == null) {
            arg.addTerm(num);
        }

        return num;
    }

    @Override
    public Term visit(AstPfxExpr pfxExpr, Subscript arg) {
        Term num = new Term(pfxExpr);
        if (currStmt == null) {
            arg.addTerm(num);
        }

        return num;
    }

    @Override
    public Term visit(AstNameExpr nameExpr, Subscript arg) {
        Term name = new Term(SemAn.definedAt.get(nameExpr));
        if (currStmt == null) {
            arg.addTerm(name);
        }

        return name;
    }

}
