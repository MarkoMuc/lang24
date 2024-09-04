package lang24.data.datadep;

import lang24.common.report.Report;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstBinExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.phase.seman.SemAn;

public class SubscriptAnalyzer implements AstFullVisitor<Subscript.Term, Subscript> {

    @Override
    public Subscript.Term visit(AstBinExpr binExpr, Subscript arg) {

        // This can be done much better with correct visitor pattern
        // All expr but Oper.ADD return their TERM
        // Oper.add returns null always
        // If a null is found and you are not also a Oper.ADD -> Break?
        // If both returns are TERM with no var add em, also only add them here with arg.add
        // Otherwise if one is term and other is var during mul, combine them as one

        if (binExpr.oper == AstBinExpr.Oper.ADD) {
            if (binExpr.fstExpr instanceof AstNameExpr name) {
                switch (binExpr.sndExpr) {
                    case AstBinExpr next -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        next.accept(this, arg);
                    }
                    case AstNameExpr name2 -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        arg.addTerm(SemAn.definedAt.get(name2));
                    }
                    case AstAtomExpr coef -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        arg.addTerm(coef);
                    }
                    case null, default ->
                        //FIXME: should return null
                            throw new Report.Error("ADD and fst is name");
                }
            } else if (binExpr.sndExpr instanceof AstNameExpr name) {
                switch (binExpr.fstExpr) {
                    case AstBinExpr next -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        next.accept(this, arg);
                    }
                    case AstAtomExpr coef -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        arg.addTerm(coef);
                    }
                    case null, default ->
                        //FIXME: should return null
                            throw new Report.Error("ADD fst no match and snd is name");
                }
            } else if (binExpr.fstExpr instanceof AstAtomExpr atom1 &&
                    binExpr.sndExpr instanceof AstAtomExpr atom2) {
                if (atom1.type != AstAtomExpr.Type.INT && atom2.type != AstAtomExpr.Type.INT) {
                    throw new Report.Error("Both aint int in add");
                } else {
                    arg.addTerm(Integer.parseInt(atom1.value) + Integer.parseInt(atom2.value));
                }
            } else if (binExpr.fstExpr instanceof AstBinExpr &&
                    binExpr.sndExpr instanceof AstBinExpr) {
                binExpr.fstExpr.accept(this, arg);
                binExpr.sndExpr.accept(this, arg);
            } else if (binExpr.fstExpr instanceof AstBinExpr) {
                binExpr.fstExpr.accept(this, arg);
                if (binExpr.sndExpr instanceof AstAtomExpr a) {
                    arg.addTerm(a);
                }
            } else if (binExpr.sndExpr instanceof AstBinExpr) {
                binExpr.sndExpr.accept(this, arg);
                if (binExpr.fstExpr instanceof AstAtomExpr a) {
                    arg.addTerm(a);
                }
            } else {
                throw new Report.Error(binExpr, "ADD matches neither");
            }

        } else if (binExpr.oper == AstBinExpr.Oper.MUL) {
            if (binExpr.fstExpr instanceof AstNameExpr name) {
                switch (binExpr.sndExpr) {
                    case AstBinExpr next -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        next.accept(this, arg);
                    }
                    //FIXME: should be illegal right, this is i*j
                    case AstNameExpr name2 -> throw new Report.Error("MUL fst name second name");
                    case AstAtomExpr coef -> arg.addTerm(SemAn.definedAt.get(name), coef);
                    case null, default ->
                        //FIXME: should return null
                            throw new Report.Error("MUL fst name second doesnt match");
                }

            } else if (binExpr.sndExpr instanceof AstNameExpr name) {
                switch (binExpr.fstExpr) {
                    case AstBinExpr next -> {
                        arg.addTerm(SemAn.definedAt.get(name));
                        next.accept(this, arg);
                    }
                    case AstAtomExpr coef -> arg.addTerm(SemAn.definedAt.get(name), coef);
                    case null, default ->
                        //FIXME: should return null
                            throw new Report.Error("MUL snd is name, fst no match");
                }
            } else if (binExpr.fstExpr instanceof AstAtomExpr atom1 &&
                    binExpr.sndExpr instanceof AstAtomExpr atom2) {
                if (atom1.type != AstAtomExpr.Type.INT && atom2.type != AstAtomExpr.Type.INT) {
                    throw new Report.Error("Both aint int in MUL");
                } else {
                    arg.addTerm(Integer.parseInt(atom1.value) * Integer.parseInt(atom2.value));
                }
            } else {
                throw new Report.Error("MUL matches neither");
            }

        } else if (binExpr.fstExpr instanceof AstBinExpr &&
                binExpr.sndExpr instanceof AstBinExpr) {
            //FIXME: is this legal?
            binExpr.fstExpr.accept(this, arg);
            binExpr.sndExpr.accept(this, arg);
        } else {
            return null;
        }
        return null;
    }

    @Override
    public Subscript.Term visit(AstAtomExpr atomExpr, Subscript arg) {
        arg.addTerm(atomExpr);
        return null;
    }

    @Override
    public Subscript.Term visit(AstNameExpr nameExpr, Subscript arg) {
        arg.addTerm(SemAn.definedAt.get(nameExpr));
        return null;
    }
}
