package lang24.phase.imcgen;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstAssignStmt;
import lang24.data.ast.tree.stmt.AstVecForStmt;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.datadep.codegen.SCCDependenceGraph;
import lang24.data.datadep.depgraph.DDGNode;
import lang24.data.datadep.depgraph.DataDependenceGraph;
import lang24.data.datadep.depgraph.StronglyConnectedComponent;
import lang24.data.datadep.subscript.Subscript;
import lang24.data.datadep.subscript.Term;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.mem.*;
import lang24.data.type.SemArrayType;
import lang24.data.type.SemPointerType;
import lang24.data.type.SemType;
import lang24.phase.memory.MemEvaluator;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;
import lang24.phase.vecan.SubscriptAnalyzer;
import lang24.phase.vecan.VecAn;

import java.util.Stack;
import java.util.Vector;

/**
 * @author marko.muc12@gmail.com
 */
public class ImcVecGen implements AstFullVisitor<Object, Stack<MemFrame>> {

    private boolean vectorize = false;
    private Vector<AstVecForStmt> loops;
    private Stack<MemFrame> arg;
    private int depth;

    private ImcSTMTS codegen(DataDependenceGraph D, int k) {
        var scc = D.TarjansSCC();

        var SCCGraph = new SCCDependenceGraph();
        SCCGraph.addSCCs(scc);
        SCCGraph.buildGraph();

        var piblocks = SCCGraph.topologicalSort();

        int maxDetph = loops.size();
        var levelk = new Vector<ImcStmt>();
        for (var piblock : piblocks) {
            if (piblock.getSize() > 1) {
                var loopContext = new LoopContext(this, loops.get(k - 1), k, arg);
                levelk.addAll(generateHeader(loopContext));

                ImcSTMTS stmts = codegen(PiGraph(piblock, k), k + 1);
                levelk.addAll(stmts.stmts);

                levelk.addAll(generateEnd(loopContext));
            } else {
                var node = piblock.getNodes().getFirst();
                var statement = node.stmt;

                vectorize = node.depth > k;
                depth = k;
                levelk.add((ImcStmt) statement.accept(this, arg));
                vectorize = false;
            }
        }
        ImcSTMTS stmts = new ImcSTMTS(levelk);
        ImcGen.stmtImc.put(loops.get(k - 1), stmts);

        return stmts;
    }

    @Override
    public Object visit(AstVecForStmt vecForStmt, Stack<MemFrame> arg) {
        this.arg = arg;
        loops = VecAn.outerToNest.get(vecForStmt);
        ImcSTMTS vectorized = codegen(VecAn.graphs.get(vecForStmt), 1);

        return vectorized;
    }

    @Override
    public Object visit(AstAssignStmt assignStmt, Stack<MemFrame> arg) {
        // Creates a VecForAssign
        // Left side is always an array expression,
        ImcStmt stmt;
        if (vectorize) {
            ImcExpr expr1 = (ImcExpr) assignStmt.src.accept(this, arg);
            ImcExpr expr2 = (ImcExpr) assignStmt.dst.accept(this, arg);
            stmt = new ImcVecMOVE(expr1, expr2);
        } else {
            stmt = (ImcStmt) assignStmt.accept(new ImcGenerator(), arg);
        }

        ImcGen.stmtImc.put(assignStmt, stmt);

        return stmt;
    }

    @Override
    public Object visit(AstMultiArrExpr multiArrExpr, Stack<MemFrame> arg) {
        // IMCGENERATOR HERE
        ImcExpr arr = (ImcExpr) multiArrExpr.arr.accept(new ImcGenerator(), arg);

        SemType semType = SemAn.ofType.get(multiArrExpr.arr).actualType();

        ImcCONST typeSize = null;
        ImcExpr imcMEM = arr;
        AstExpr lastIdx = null;

        //We only vectorize the last index, since it has to depend on some loop or value for the other indexes
        if (semType instanceof SemPointerType ptrType) {
            SemType type = ptrType;
            var size = 0;
            for (var idx : multiArrExpr.idxs) {
                lastIdx = idx;
                if (vectorize && size == multiArrExpr.idxs.size() - 1) {
                    break;
                }
                // IMCGENERATOR HERE
                ImcExpr idxImcExpr = (ImcExpr) idx.accept(new ImcGenerator(), arg);
                typeSize = new ImcCONST(MemEvaluator.SizeOfType(ptrType.baseType));
                ImcBINOP imcOffset = new ImcBINOP(ImcBINOP.Oper.MUL, idxImcExpr, typeSize);

                imcMEM = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, imcMEM, imcOffset));

                if (type instanceof SemPointerType tmpType) {
                    type = tmpType.baseType;
                }

                size++;
            }
        } else {
            throw new Report.Error(multiArrExpr, "This shouldn't happen.");
        }

        if (vectorize) {
            Subscript lastSubscript = VecAn.exprToSubscript.get(lastIdx);
            if (lastSubscript == null) {
                lastSubscript = new Subscript();
                assert lastIdx != null;
                lastIdx.accept(new SubscriptAnalyzer(), lastSubscript);
            }

            ImcExpr start = genVecStart(lastSubscript, depth);
            ImcExpr end = genVecEnd(start, lastSubscript, depth);

            imcMEM = new ImcVecMEM(imcMEM, start, end);
        }

        ImcGen.exprImc.put(multiArrExpr, imcMEM);

        return imcMEM;
    }

    @Override
    public Object visit(AstArrExpr arrExpr, Stack<MemFrame> arg) {
        // IMCGENERATOR HERE
        ImcExpr arr = (ImcExpr) arrExpr.arr.accept(new ImcGenerator(), arg);
        ImcExpr idx = (ImcExpr) arrExpr.idx.accept(new ImcGenerator(), arg);

        SemType semType = SemAn.ofType.get(arrExpr.arr).actualType();

        ImcCONST typeSize = null;
        if (semType instanceof SemArrayType arrType) {
            typeSize = new ImcCONST(MemEvaluator.SizeOfType(arrType.elemType));
            if (arr instanceof ImcMEM mem) {
                arr = mem.addr;
            }
        } else if (semType instanceof SemPointerType ptrType) {
            typeSize = new ImcCONST(MemEvaluator.SizeOfType(ptrType.baseType));
        } else {
            throw new Report.Error(arrExpr, "This shouldn't happen.");
        }

        if (vectorize) {
            // If this array ref is vectorized, we only need the address of the array

            Subscript lastSubscript = VecAn.exprToSubscript.get(arrExpr.idx);
            if (lastSubscript == null) {
                lastSubscript = new Subscript();
                arrExpr.idx.accept(new SubscriptAnalyzer(), lastSubscript);
                //No dependence, so it was not analyzed
            }

            ImcExpr start = genVecStart(lastSubscript, depth);
            ImcExpr end = genVecEnd(start, lastSubscript, depth);

            arr = new ImcVecMEM(arr, start, end);

            ImcGen.exprImc.put(arrExpr, arr);
            return arr;
        }

        ImcBINOP imcOffset = new ImcBINOP(ImcBINOP.Oper.MUL, idx, typeSize);
        ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, arr, imcOffset);
        ImcMEM imcMEM = new ImcMEM(imcBin);

        ImcGen.exprImc.put(arrExpr, arr);

        return imcMEM;
    }

    @Override
    public Object visit(AstBinExpr binExpr, Stack<MemFrame> arg) {
        ImcExpr expr1 = (ImcExpr) binExpr.fstExpr.accept(this, arg);
        ImcExpr expr2 = (ImcExpr) binExpr.sndExpr.accept(this, arg);

        ImcExpr imcBin;
        if (vectorize) {
            ImcVecBINOP.Oper oper = switch (binExpr.oper) {
                case ADD -> ImcVecBINOP.Oper.ADD;
                case SUB -> ImcVecBINOP.Oper.SUB;
                case MUL -> ImcVecBINOP.Oper.MUL;
                case DIV -> ImcVecBINOP.Oper.DIV;
                case MOD -> ImcVecBINOP.Oper.MOD;
                default -> throw new Report.Error("Wrong operand.");
            };
            imcBin = new ImcVecBINOP(oper, expr1, expr2);
        } else {
            imcBin = (ImcExpr) binExpr.accept(new ImcGenerator(), arg);
        }

        ImcGen.exprImc.put(binExpr, imcBin);

        return imcBin;
    }

    @Override
    public Object visit(AstNameExpr nameExpr, Stack<MemFrame> arg) {
        AstDefn astDefn = SemAn.definedAt.get(nameExpr);
        ImcExpr imcExpr = null;

        MemAccess memAccess;
        if (astDefn instanceof AstVarDefn astVarDefn) {
            memAccess = Memory.varAccesses.get(astVarDefn);
        } else if (astDefn instanceof AstFunDefn.AstParDefn parDefn) {
            memAccess = Memory.parAccesses.get(parDefn);
        } else {
            return null;
        }

        if (memAccess instanceof MemAbsAccess) {
            imcExpr = new ImcMEM(new ImcNAME(((MemAbsAccess) memAccess).label));
        } else {
            MemRelAccess local = (MemRelAccess) memAccess;
            ImcExpr expr = new ImcTEMP(arg.peek().FP);

            long depth = arg.peek().depth - local.depth;
            ImcCONST imcCONST = new ImcCONST(local.offset);

            for (int i = 0; i < depth; i++) {
                expr = new ImcMEM(expr);
            }

            ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, expr, imcCONST);
            imcExpr = new ImcMEM(imcBin);
            if (astDefn instanceof AstFunDefn.AstRefParDefn) {
                imcExpr = new ImcMEM(imcExpr);
            }
        }
        ImcGen.exprImc.put(nameExpr, imcExpr);

        return imcExpr;
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, Stack<MemFrame> arg) {
        ImcExpr imcConst;

        if (atomExpr.type == AstAtomExpr.Type.INT) {
            imcConst = new ImcCONST(ImcGenerator.checkAndParse(atomExpr.value));
        } else {
            throw new Report.Error("Non integer atom in expression");
        }

        ImcGen.exprImc.put(atomExpr, imcConst);
        return imcConst;
    }

    private DataDependenceGraph PiGraph(StronglyConnectedComponent SCC, int k) {
        var graph = new DataDependenceGraph();

        var SCCNodes = SCC.getNodes();
        for (var SCCNode : SCCNodes) {
            var node = new DDGNode(SCCNode.stmt, SCCNode.depth, SCCNode.stmtNum);
            for (var conns : SCCNode.connections) {
                if (conns.directionVector.depLevel == 0) {
                    node.addConnection(conns);
                    continue;
                }

                if (conns.directionVector.depLevel > k && SCCNodes.contains(conns.sink)) {
                    node.addConnection(conns);
                }
            }
            graph.addDDGNode(node);
        }

        return graph;
    }

    private ImcExpr genVecStart(Subscript subscript, int k) {
        ImcExpr binExpr;
        Vector<Term> terms = new Vector<>(subscript.getTerms());
        if (!terms.isEmpty() && terms.getFirst().depth < k) {
            binExpr = (ImcExpr) terms.getFirst().name.accept(this, arg);
            binExpr = new ImcBINOP(ImcBINOP.Oper.MUL, new ImcCONST(terms.getFirst().coefficient), binExpr);
            terms.removeFirst();
        } else {
            binExpr = new ImcCONST(0);
        }

        for (var term : terms) {
            if (term.depth < k) {
                ImcExpr variable = (ImcExpr) term.name.accept(this, arg);
                variable = new ImcBINOP(ImcBINOP.Oper.MUL, new ImcCONST(term.coefficient), variable);
                binExpr = new ImcBINOP(ImcBINOP.Oper.ADD, variable, binExpr);
            }
        }
        if (subscript.getConstant() != null && subscript.getConstant().coefficient != 0) {
            binExpr = new ImcBINOP(ImcBINOP.Oper.ADD, new ImcCONST(subscript.getConstant().coefficient), binExpr);
        }

        return binExpr;
    }

    private ImcExpr genVecEnd(ImcExpr start, Subscript subscript, int k) {
        int end = 100;

        Vector<Term> terms = new Vector<>(subscript.getTerms());
        for (var term : terms) {
            if (term.depth > k) {
                var loop = VecAn.loopDescriptors.get((AstNameExpr) term.name);
                end = loop.upperBound - 1;
                break;
            }
        }
        if (start instanceof ImcCONST con) {
            if (con.value == 0) {
                return new ImcCONST(end);
            }
        }

        return new ImcBINOP(ImcBINOP.Oper.ADD, start, new ImcCONST(end));
    }


    private class LoopContext {
        ImcVecGen visitor;
        AstVecForStmt stmt;
        ImcExpr name;
        ImcExpr lower;
        ImcExpr upper;
        ImcExpr step;
        MemLabel cond;
        MemLabel body;
        MemLabel end;
        int depth;

        public LoopContext(ImcVecGen visitor, AstVecForStmt stmt, int k, Stack<MemFrame> arg) {
            this.visitor = visitor;
            this.stmt = stmt;
            this.depth = k;
            initialize(arg);
        }

        public void initialize(Stack<MemFrame> arg) {
            this.name = (ImcExpr) this.stmt.name.accept(visitor, arg);
            this.lower = (ImcExpr) this.stmt.lower.accept(visitor, arg);
            this.upper = (ImcExpr) this.stmt.upper.accept(visitor, arg);
            this.step = (ImcExpr) this.stmt.step.accept(visitor, arg);

            this.cond = new MemLabel();
            this.body = new MemLabel();
            this.end = new MemLabel();
        }
    }

    private Vector<ImcStmt> generateHeader(LoopContext loop) {
        Vector<ImcStmt> stmts = new Vector<>();
        ImcStmt imcInitStmt = new ImcMOVE(loop.name, loop.lower);
        ImcExpr condExpr = new ImcBINOP(ImcBINOP.Oper.LTH, loop.name, loop.upper);

        stmts.add(imcInitStmt);
        stmts.add(new ImcLABEL(loop.cond));
        stmts.add(new ImcCJUMP(condExpr, loop.body, loop.end));
        stmts.add(new ImcLABEL(loop.body));
        return stmts;
    }

    private Vector<ImcStmt> generateEnd(LoopContext loop) {
        Vector<ImcStmt> stmts = new Vector<>();
        ImcStmt imcStepStmt = new ImcMOVE(loop.name, new ImcBINOP(ImcBINOP.Oper.ADD, loop.name, loop.step));

        stmts.add(imcStepStmt);
        stmts.add(new ImcJUMP(loop.cond));
        stmts.add(new ImcLABEL(loop.end));

        return stmts;
    }
}
