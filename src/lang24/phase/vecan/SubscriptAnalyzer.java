package lang24.phase.vecan;

import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstBinExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.expr.AstPfxExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.datadep.subscript.Subscript;
import lang24.data.datadep.subscript.Term;
import lang24.phase.seman.SemAn;

// CHECKME: Need to check what happens when there is more than 1 -symbol for constants

public class SubscriptAnalyzer implements AstFullVisitor<Term, Subscript> {

    private Term previous1;
    private Term previous2;
    private AstBinExpr currStmt = null;

    @Override
    public Term visit(AstBinExpr binExpr, Subscript arg) {
        if (!arg.isLinear()) {
            return null;
        }

        AstBinExpr prevExpr = currStmt;
        currStmt = binExpr;

        Term fstTerm = binExpr.fstExpr.accept(this, arg);
        Term sndTerm = binExpr.sndExpr.accept(this, arg);

        currStmt = prevExpr;

        if (!(binExpr.oper == AstBinExpr.Oper.ADD
                || binExpr.oper == AstBinExpr.Oper.MUL
                || binExpr.oper == AstBinExpr.Oper.SUB)) {
            arg.setNonLinear();
            return null;
        }

        if (fstTerm == null && sndTerm == null) {
            if (binExpr.oper != AstBinExpr.Oper.ADD) {
                //Is this true?
                arg.setNonLinear();
            }
            return null;
        } else if (fstTerm != null && sndTerm != null) {
            if (binExpr.oper == AstBinExpr.Oper.ADD || binExpr.oper == AstBinExpr.Oper.SUB) {
                if (fstTerm.variable == null && sndTerm.variable == null) {
                    // CONST + CONST
                    if (binExpr.oper == AstBinExpr.Oper.ADD) {
                        Term t = new Term(fstTerm.coefficient + sndTerm.coefficient);
                        if (currStmt == null) {
                            arg.addTerm(t);
                            return null;
                        }
                        return t;
                    } else {
                        return new Term(fstTerm.coefficient - sndTerm.coefficient);
                    }
                }
                // CONST + VAR
                // VAR + CONST
                // VAR + VAR
                if (prevExpr == null || prevExpr.oper != AstBinExpr.Oper.MUL) {
                    arg.addTerm(fstTerm);
                    //FIXME: I can just do sndTerm.coef -1 here and should fix everything?
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
                    Term t = new Term(fstTerm.coefficient * sndTerm.coefficient);
                    if (currStmt == null) {
                        arg.addTerm(t);
                    } else {
                        return t;
                    }
                } else if (fstTerm.variable != null && sndTerm.variable != null) {
                    // VAR * VAR
                    arg.setNonLinear();
                    return null;
                } else {
                    // CONST * VAR
                    // VAR * CONST
                    Term t = new Term(fstTerm, sndTerm);
                    if (currStmt == null) {
                        arg.addTerm(t);
                    } else {
                        return t;
                    }
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
                    arg.setNonLinear();
                    return null;
                }

                if (previous1 != null) {
                    arg.addTerm(new Term(previous1.variable,
                            previous1.coefficient * fstTerm.coefficient, previous1.depth));
                }

                if (previous2 != null) {
                    arg.addTerm(new Term(previous2.variable,
                            previous2.coefficient * fstTerm.coefficient, previous2.depth));
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
                    arg.setNonLinear();
                    return null;
                }

                if (previous1 != null) {
                    Term a = new Term(previous1.variable,
                            previous1.coefficient * sndTerm.coefficient, previous1.depth);
                    arg.addTerm(a);
                }

                if (previous2 != null) {
                    Term a = new Term(previous2.variable,
                            previous2.coefficient * sndTerm.coefficient, previous2.depth);
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
        Term num;

        if (pfxExpr.expr instanceof AstAtomExpr) {
            num = new Term(pfxExpr);
        } else if (pfxExpr.expr instanceof AstNameExpr name) {
            num = new Term(pfxExpr, SemAn.definedAt.get(name),
                    VecAn.loopDescriptors.get(name).getDepth());
        } else {
            arg.setNonLinear();
            return null;
        }

        if (currStmt == null) {
            arg.addTerm(num);
        }

        return num;
    }

    @Override
    public Term visit(AstNameExpr nameExpr, Subscript arg) {
        Term name = new Term(SemAn.definedAt.get(nameExpr),
                VecAn.loopDescriptors.get(nameExpr));
        if (currStmt == null) {
            arg.addTerm(name);
        }

        return name;
    }

}
