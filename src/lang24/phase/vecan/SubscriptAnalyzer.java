package lang24.phase.vecan;

import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstBinExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.expr.AstPfxExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.datadep.Subscript;
import lang24.data.datadep.Term;
import lang24.phase.seman.SemAn;

//FIXME: Issue with where a loop variable is defined. If it is defined in another loop,
//      the AstDefn is linked to that loop and any other that uses it, when it should be linked with the
//      loopDescriptor somehow

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
                //IS this true?
                arg.setNonLinear();
            }
            return null;
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
                    arg.terms.remove(previous1);
                    //arg.removeTermHash(previous1);
                    arg.addTerm(new Term(previous1.variable,
                            previous1.coefficient * fstTerm.coefficient, previous1.depth));
                }

                if (previous2 != null) {
                    arg.terms.remove(previous2);
                    //arg.removeTermHash(previous2);
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
                    arg.terms.remove(previous1);
                    //arg.removeTermHash(previous1);
                    Term a = new Term(previous1.variable,
                            previous1.coefficient * sndTerm.coefficient, previous1.depth);
                    arg.addTerm(a);
                }

                if (previous2 != null) {
                    arg.terms.remove(previous2);
                    //arg.removeTermHash(previous2);
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
                    VecAn.loopDescriptors.get(name).depth);
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
                VecAn.loopDescriptors.get(nameExpr).depth);
        if (currStmt == null) {
            arg.addTerm(name);
        }

        return name;
    }

}
