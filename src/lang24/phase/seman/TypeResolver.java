package lang24.phase.seman;

import java.util.*;
import java.util.jar.Attributes;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;
import lang24.data.type.*;
//TODO this.c = that.c
/**
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class TypeResolver implements AstFullVisitor<SemType, TypeResolver.Context> {

	/**
	 * Structural equivalence of types.
	 * 
	 * @param type1 The first type.
	 * @param type2 The second type.
	 * @return {@code true} if the types are structurally equivalent, {@code false}
	 *         otherwise.
	 */
	private boolean equiv(SemType type1, SemType type2) {
		return equiv(type1, type2, new HashMap<SemType, HashSet<SemType>>());
	}

	/**
	 * Structural equivalence of types.
	 * 
	 * @param type1  The first type.
	 * @param type2  The second type.
	 * @param equivs Type synonyms assumed structurally equivalent.
	 * @return {@code true} if the types are structurally equivalent, {@code false}
	 *         otherwise.
	 */
	private boolean equiv(SemType type1, SemType type2, HashMap<SemType, HashSet<SemType>> equivs) {

		if ((type1 instanceof SemNameType) && (type2 instanceof SemNameType)) {
			if (equivs == null)
				equivs = new HashMap<SemType, HashSet<SemType>>();

			if (equivs.get(type1) == null)
				equivs.put(type1, new HashSet<SemType>());
			if (equivs.get(type2) == null)
				equivs.put(type2, new HashSet<SemType>());
			if (equivs.get(type1).contains(type2) && equivs.get(type2).contains(type1))
				return true;
			else {
				HashSet<SemType> types;

				types = equivs.get(type1);
				types.add(type2);
				equivs.put(type1, types);

				types = equivs.get(type2);
				types.add(type1);
				equivs.put(type2, types);
			}
		}

		type1 = type1.actualType();
		type2 = type2.actualType();

		if (type1 instanceof SemVoidType)
			return (type2 instanceof SemVoidType);
		if (type1 instanceof SemBoolType)
			return (type2 instanceof SemBoolType);
		if (type1 instanceof SemCharType)
			return (type2 instanceof SemCharType);
		if (type1 instanceof SemIntType)
			return (type2 instanceof SemIntType);

		if (type1 instanceof SemArrayType) {
			if (!(type2 instanceof SemArrayType))
				return false;
			final SemArrayType arr1 = (SemArrayType) type1;
			final SemArrayType arr2 = (SemArrayType) type2;
			if (arr1.size != arr2.size)
				return false;
			return equiv(arr1.elemType, arr2.elemType, equivs);
		}

		if (type1 instanceof SemPointerType) {
			if (!(type2 instanceof SemPointerType))
				return false;
			final SemPointerType ptr1 = (SemPointerType) type1;
			final SemPointerType ptr2 = (SemPointerType) type2;
			if ((ptr1.baseType.actualType() instanceof SemVoidType)
					|| (ptr2.baseType.actualType() instanceof SemVoidType))
				return true;
			return equiv(ptr1.baseType, ptr2.baseType, equivs);
		}

		if (type1 instanceof SemStructType) {
			if (!(type2 instanceof SemStructType))
				return false;
			final SemStructType str1 = (SemStructType) type1;
			final SemStructType str2 = (SemStructType) type2;
			if (str1.cmpTypes.size() != str2.cmpTypes.size())
				return false;
			for (int c = 0; c < str1.cmpTypes.size(); c++)
				if (!(equiv(str1.cmpTypes.get(c), str2.cmpTypes.get(c), equivs)))
					return false;
			return true;
		}
		if (type1 instanceof SemUnionType) {
			if (!(type2 instanceof SemUnionType))
				return false;
			final SemUnionType uni1 = (SemUnionType) type1;
			final SemUnionType uni2 = (SemUnionType) type2;
			if (uni1.cmpTypes.size() != uni2.cmpTypes.size())
				return false;
			for (int c = 0; c < uni1.cmpTypes.size(); c++)
				if (!(equiv(uni1.cmpTypes.get(c), uni2.cmpTypes.get(c), equivs)))
					return false;
			return true;
		}

		throw new Report.InternalError();
	}

	public enum Context{
		FIRST, SECOND, THIRD
	}
	private HashMap<AstRecType, SymbTable> StrTables = new HashMap<>();
	private HashMap<SemType, SymbTable> StrTypesTables = new HashMap<>();
	private Stack<SymbTable> StrTableStack = new Stack<>();
	private Stack<Vector<SemType>> CmpVecStack= new Stack<>();
	private Stack<SemType> FuncTypeStack = new Stack<>();
	private SemNameType DefNameType = null;

	@Override
	public SemType visit(AstAtomType atomType, Context arg) {
		SemType type = switch (atomType.type) {
			case VOID -> SemVoidType.type;
			case BOOL -> SemBoolType.type;
			case CHAR -> SemCharType.type;
			case INT -> SemIntType.type;
		};

		SemAn.isType.put(atomType, type);
		return type;
	}

	@Override
	public SemType visit(AstArrType arrType, Context arg) {
		SemType ArrType= arrType.elemType.accept(this, null);
		SemType SizeType = arrType.size.accept(this, null);

		if (ArrType instanceof SemVoidType) {
			throw new Report.Error(arrType,"Array type cannot be void.");
		}

		if (SizeType.actualType() instanceof SemIntType) {
			if(arrType.size instanceof AstAtomExpr IntConst
				&& IntConst.type == AstAtomExpr.Type.INT) {
				Long LongSize = Long.parseLong(IntConst.value);
				if (LongSize > 0 && LongSize < Math.pow(2.0, 63)){
					SemType array = new SemArrayType(ArrType, LongSize);
					SemAn.isType.put(arrType, array);
					return array;
				}else {
					throw new Report.Error(arrType,"Array size must be in range 0 < X < 2^63-1.");
				}
			}else {
				throw new Report.Error(arrType,"Array size must be an integer ");
			}
		}else {
			throw new Report.Error(arrType,"Array size must be an integer constant.");
		}
	}

	@Override
	public SemType visit(AstPtrType ptrType, Context arg) {
		SemType PtrType = ptrType.baseType.accept(this, null);
		SemPointerType pointer = new SemPointerType(PtrType);
		SemAn.isType.put(ptrType, pointer);

		return pointer;
	}

	@Override
	public SemType visit(AstStrType strType, Context arg) {
		SemStructType type = null;
		SymbTable StrTable = new SymbTable();

		CmpVecStack.push(new Vector<>());
		StrTableStack.push(StrTable);
		StrTables.put(strType, StrTable);

		strType.cmps.accept(this, null);

		type = new SemStructType(CmpVecStack.pop());

		StrTypesTables.put(type, StrTableStack.pop());
		SemAn.isType.put(strType, type);

		return type;
	}

	@Override
	public SemType visit(AstUniType uniType, Context arg) {
		SemUnionType type = null;
		SymbTable StrTable = new SymbTable();

		CmpVecStack.push(new Vector<>());
		StrTableStack.push(StrTable);
		StrTables.put(uniType, StrTable);

		uniType.cmps.accept(this, null);

		type = new SemUnionType(CmpVecStack.pop());

		StrTypesTables.put(type, StrTableStack.pop());
		SemAn.isType.put(uniType, type);

		return type;
	}

	@Override
	public SemType visit(AstNameType nameType, Context arg) {
		AstDefn defn = SemAn.definedAt.get(nameType);
		SemType type = null;

		if(defn instanceof AstTypDefn){
			type = SemAn.isType.get(defn);
			SemAn.isType.put(nameType, type);
		}else{
			throw new Report.Error(nameType,nameType.name + " is not a type.");
		}

		return type;
	}

	@Override
	public SemType visit(AstAtomExpr atomExpr, Context arg) {
		SemType type = switch (atomExpr.type) {
			case VOID -> SemVoidType.type;
			case PTR -> new SemPointerType(SemVoidType.type);
			case STR -> new SemPointerType(SemCharType.type);
			case BOOL -> SemBoolType.type;
			case CHAR -> SemCharType.type;
			case INT -> SemIntType.type;
		};
		SemAn.ofType.put(atomExpr, type);

		return type;
	}

	@Override
	public SemType visit(AstPfxExpr pfxExpr, Context arg) {
		SemType ExprType = pfxExpr.expr.accept(this, null);
		SemType type = null;
		if (pfxExpr.oper == AstPfxExpr.Oper.NOT &&
				ExprType.actualType() instanceof SemBoolType){
			type = SemBoolType.type;
		} else if ((pfxExpr.oper == AstPfxExpr.Oper.ADD ||
				pfxExpr.oper == AstPfxExpr.Oper.SUB) &&
				ExprType.actualType() instanceof SemIntType) {
			type = SemIntType.type;
		} else if (pfxExpr.oper == AstPfxExpr.Oper.PTR) {
			type = new SemPointerType(ExprType);
		} else {
			throw new Report.Error(pfxExpr,"Prefix expression type error.");
		}
		SemAn.ofType.put(pfxExpr, type);
		return type;
	}

	@Override
	public SemType visit(AstBinExpr binExpr, Context arg) {
		SemType FstExprType = binExpr.fstExpr.accept(this, null);
		SemType SndExprType = binExpr.sndExpr.accept(this, null);
		SemType type = null;
		switch (binExpr.oper) {
			case AND, OR -> {
				if(FstExprType.actualType() instanceof SemBoolType &&
						SndExprType.actualType() instanceof SemBoolType){
					type = SemBoolType.type;
				}else {
					throw new Report.Error(binExpr, "Binary Bitwise Expression Type Error!");
				}
			}
			case ADD, SUB, MUL, DIV, MOD -> {
				if(FstExprType.actualType() instanceof SemIntType &&
						SndExprType.actualType() instanceof SemIntType){
					type = SemIntType.type;
				}else {
					throw new Report.Error(binExpr, "Binary Integer Expression Type Error!");
				}
			}
			case EQU, NEQ -> {
				// TODO : Does equiv already check if the pointers point to same type?
				if(equiv(FstExprType, SndExprType) && (
						FstExprType.actualType() instanceof SemBoolType ||
								FstExprType.actualType() instanceof SemIntType ||
								FstExprType.actualType() instanceof SemCharType ||
								FstExprType.actualType() instanceof SemPointerType
						)){
					type = SemBoolType.type;
				}else {
					throw new Report.Error(binExpr, "Equivalence expression type error!");
				}
			}
			case LEQ, GEQ, LTH, GTH -> {
				if(equiv(FstExprType, SndExprType) && (
								FstExprType.actualType() instanceof SemIntType ||
								FstExprType.actualType() instanceof SemCharType ||
								FstExprType.actualType() instanceof SemPointerType
				)){
					type = SemBoolType.type;
				}else {
					throw new Report.Error(binExpr, "Value comparison expression type error!");
				}
			}
		}
		SemAn.ofType.put(binExpr, type);
		return type;
	}

	@Override
	public SemType visit(AstSfxExpr sfxExpr, Context arg) {
		// TODO: check how this handles pointer to name type?
		SemType ExprType = sfxExpr.expr.accept(this, null);
		SemType type = null;

		if(ExprType.actualType() instanceof SemPointerType PtrType){
			type = PtrType.baseType;
			SemAn.ofType.put(sfxExpr, type);
		}else {
			throw new Report.Error(sfxExpr, "Type Is Not Compatible with SuffixExpression!");
		}

		return type;
	}

	@Override
	public SemType visit(AstArrExpr arrExpr, Context arg) {
		// TODO: Lvalue
		SemType Expr1Type = arrExpr.arr.accept(this, null);
		SemType Expr2Type = arrExpr.idx.accept(this, null);
		SemType type = null;

		if(Expr1Type.actualType() instanceof SemArrayType ArrType){
			if(!(Expr2Type.actualType() instanceof SemIntType)){
				throw new Report.Error(arrExpr, "Id expression must be type int.");
			}
			type = ArrType.elemType;
		}else {
			throw new Report.Error(arrExpr, "Expression is not type array.");
		}

		SemAn.ofType.put(arrExpr, type);
		return type;
	}

	@Override
	public SemType visit(AstCastExpr castExpr, Context arg) {
		SemType CastType = castExpr.type.accept(this, null);
		SemType ExprType = castExpr.expr.accept(this, null);
		if(!(CastType.actualType() instanceof SemCharType ||
				CastType.actualType() instanceof SemIntType ||
				CastType.actualType() instanceof SemPointerType)){
			throw new Report.Error(castExpr,"Cast Type Error");
		}

		if(!(ExprType.actualType() instanceof SemCharType ||
				ExprType.actualType() instanceof SemIntType ||
				ExprType.actualType() instanceof SemPointerType)){
			throw new Report.Error(castExpr,"Cast Type Error");
		}

		SemAn.ofType.put(castExpr, CastType.actualType());

		return CastType;
	}

	@Override
	public SemType visit(AstNameExpr nameExpr, Context arg) {
		AstDefn def = SemAn.definedAt.get(nameExpr);

		SemType type = null;
		if(def instanceof AstVarDefn ||
				def instanceof AstFunDefn ||
				def instanceof AstFunDefn.AstParDefn){
			type = SemAn.ofType.get(def);
		} else if (def instanceof AstTypDefn) {
			throw new Report.Error(nameExpr,"Type as expression error!");
		}else {
			type = SemAn.isType.get(def);
		}
		SemAn.ofType.put(nameExpr, type);

		return type;
	}

	@Override
	public SemType visit(AstSizeofExpr sizeofExpr, Context arg) {
		SemType SizeType = sizeofExpr.type.accept(this, null);
		SemType type = SemIntType.type;
		SemAn.ofType.put(sizeofExpr, type);

		return type;
	}

	@Override
	public SemType visit(AstCmpExpr cmpExpr, Context arg) {
		SemType ExprType = cmpExpr.expr.accept(this, null);
		SemType type = null;
		if(!(ExprType.actualType() instanceof SemRecordType)){
			throw new Report.Error(cmpExpr, "This is not a struct.");
		}
		// Make sure its right type!
		SymbTable table = StrTypesTables.get(ExprType.actualType());

        try {
            AstDefn cmpDefn = table.fnd(cmpExpr.name);
			type = SemAn.ofType.get(cmpDefn);
        } catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(cmpExpr,cmpExpr.name + " component not found.");
        }

        SemAn.ofType.put(cmpExpr, type);
		return type;
	}

	@Override
	public SemType visit(AstCallExpr callExpr, Context arg) {
		AstFunDefn defn = (AstFunDefn) SemAn.definedAt.get(callExpr);
		SemType FuncType = SemAn.ofType.get(defn);
		SemAn.ofType.put(callExpr, FuncType);

		Vector<SemType> ArgTypes = new Vector<>();

		for(AstFunDefn.AstParDefn par : defn.pars){
			ArgTypes.add(SemAn.ofType.get(par));
		}

		if(ArgTypes.size() != callExpr.args.size()){
			throw new Report.Error(callExpr,"Parameter count does not match argument count.");
		}

		int i = 0;
		for(AstExpr expr : callExpr.args){
			SemType t1 = expr.accept(this, null);
			////FIXME is this needed
			//if (defn.pars.get(i) instanceof AstFunDefn.AstRefParDefn){
			//	Boolean test = SemAn.isLVal.get(expr);

			//	if(test == null || !test){
			//		throw new Report.Error(callExpr,"Expression is not an L-value for RefPar.");
			//	}
			//}
			if(!equiv(t1, ArgTypes.get(i))){
				throw new Report.Error(callExpr,"Parameter type mismatch.");
			}
			i++;
		}
		return FuncType;
	}


	@Override
	public SemType visit(AstAssignStmt assignStmt, Context arg) {
		SemType SrcType = assignStmt.src.accept(this, null);
		SemType DstType = assignStmt.dst.accept(this, null);

		if(equiv(SrcType, DstType) &&
				(SrcType.actualType() instanceof SemBoolType ||
						SrcType.actualType() instanceof SemIntType ||
						SrcType.actualType() instanceof SemCharType ||
						SrcType.actualType() instanceof SemPointerType)){
			SemAn.ofType.put(assignStmt, SemVoidType.type);
		}else{
			throw new Report.Error(assignStmt, "Assign Statement Type Error!");
		}
		return SemVoidType.type;
	}

	@Override
	public SemType visit(AstExprStmt exprStmt, Context arg) {
		SemType type = exprStmt.expr.accept(this, null);
		if(!(type.actualType() instanceof SemVoidType)) {
			throw new Report.Error(exprStmt, "Expression statement type error.");
		}

		SemAn.ofType.put(exprStmt, type.actualType());

		return type.actualType();
	}

	@Override
	public SemType visit(AstIfStmt ifStmt, Context arg) {
		SemType CondType = ifStmt.cond.accept(this, null);
		SemType ThenType = ifStmt.thenStmt.accept(this, null);
		SemType ElseType = null;

		if(CondType.actualType() instanceof SemBoolType){
			//TODO:Is this needed?
			//SemAn.ofType.put(ifStmt.cond, CondType.actualType());
		}else{
			throw new Report.Error(ifStmt, "If Statement Condition Type Error.");
		}

		//TODO:Is this needed?
		//SemAn.ofType.put(ifStmt.thenStmt, ThenType.actualType());
		if(ifStmt.elseStmt != null){
			//TODO:Is this needed?
			ElseType = ifStmt.elseStmt.accept(this, null);
			//SemAn.ofType.put(ifStmt.elseStmt, ElseType.actualType());
		}
		SemAn.ofType.put(ifStmt, SemVoidType.type);

		return SemVoidType.type;
	}

	@Override
	public SemType visit(AstWhileStmt whileStmt, Context arg) {
		//TODO:DO you need to assign oftype?
		SemType CondType = whileStmt.cond.accept(this, null);
		SemType StmtType = whileStmt.stmt.accept(this, null);

		if(!(CondType.actualType() instanceof SemBoolType)){
			throw new Report.Error(whileStmt, "While condition is not type bool.");
		}
		SemAn.ofType.put(whileStmt, SemVoidType.type);

		return SemVoidType.type;
	}

	@Override
	public SemType visit(AstReturnStmt retStmt, Context arg) {
		SemType ExprType = retStmt.expr.accept(this, null);
		if(!equiv(FuncTypeStack.peek(), ExprType)){
			throw new Report.Error(retStmt, "Return type does not match.");
		}
		SemAn.ofType.put(retStmt, SemVoidType.type);

		return SemVoidType.type;
	}

	@Override
	public SemType visit(AstBlockStmt blockStmt, Context arg) {
		for (AstStmt stmt : blockStmt.stmts){
			stmt.accept(this, null);
		}
		return SemVoidType.type;
	}

	@Override
	public SemType visit(AstTypDefn typDefn, Context arg) {
		SemType type = null;
		if(arg == Context.FIRST){
			SemNameType NameType = new SemNameType(typDefn.name);
			SemAn.isType.put(typDefn, NameType);
		} else if (arg == Context.SECOND) {
			DefNameType = (SemNameType) SemAn.isType.get(typDefn);
			type = typDefn.type.accept(this, null);
			try {
				if(DefNameType == type) {
					throw new Report.Error(typDefn, typDefn.name + " is a cyclic type");
				}
				if(type instanceof SemNameType) type.actualType();
			}catch (Exception e){
				throw new Report.Error(typDefn, typDefn.name + " is a cyclic type.");
			}

			if(SemAn.isType.get(typDefn) instanceof SemNameType NameType){
				NameType.define(type);
			}else {
				throw new Report.Error(typDefn, "Unexpected type name error");
			}
		}
		return type;
	}

	@Override
	public SemType visit(AstVarDefn varDefn, Context arg) {
		SemType type = varDefn.type.accept(this, null);
		if(type.actualType() instanceof SemVoidType) {
			throw new Report.Error(varDefn, varDefn.name + " is type void.");
		}
		SemAn.ofType.put(varDefn, type);
		return type;
	}

	@Override
	public SemType visit(AstRecType.AstCmpDefn cmpDefn, Context arg) {
		SemType type = cmpDefn.type.accept(this, null);
		try {
			if(type.actualType() instanceof SemVoidType) {
				throw new Report.Error(cmpDefn, cmpDefn.name + " is type void.");
			}
		}catch (Exception e){
			throw new Report.Error(cmpDefn, cmpDefn.name + " is a cyclic type.");
		}
        try {
			StrTableStack.peek().ins(cmpDefn.name, cmpDefn);
        } catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(cmpDefn, cmpDefn.name + " component identifier already in use.");
        }

        SemAn.ofType.put(cmpDefn, type);
		return type;
	}

	@Override
	public SemType visit(AstFunDefn.AstRefParDefn refParDefn, Context arg) {
		SemType type = refParDefn.type.accept(this, null);
		if(type.actualType() instanceof SemVoidType) {
			throw new Report.Error(refParDefn, refParDefn.name + " is type void.");
		}
		SemType ptrType = new SemPointerType(type);
		SemAn.ofType.put(refParDefn, ptrType);
		return ptrType;
	}

	@Override
	public SemType visit(AstFunDefn.AstValParDefn valParDefn, Context arg) {
		SemType type = valParDefn.type.accept(this, null);
		if(type.actualType() instanceof SemVoidType) {
			throw new Report.Error(valParDefn, valParDefn.name + " is type void.");
		}
		SemAn.ofType.put(valParDefn, type);
		return type;
	}

	@Override
	public SemType visit(AstFunDefn funDefn, Context arg) {
		SemType RetType = null;
		if(arg == Context.SECOND) {
			RetType = funDefn.type.accept(this, null);

			if (!(RetType.actualType() instanceof SemBoolType ||
					RetType.actualType() instanceof SemCharType ||
					RetType.actualType() instanceof SemIntType ||
					RetType.actualType() instanceof SemVoidType ||
					RetType.actualType() instanceof SemPointerType)) {
				throw new Report.Error(funDefn, funDefn.name + " is wrong type.");
			}
			if (funDefn.pars != null) {
				for (AstFunDefn.AstParDefn par : funDefn.pars) {
					SemType type = par.accept(this, null);
					if (!(type.actualType() instanceof SemBoolType ||
							type.actualType() instanceof SemCharType ||
							type.actualType() instanceof SemIntType ||
							type.actualType() instanceof SemPointerType)) {
						throw new Report.Error(funDefn, par.name + " is wrong type.");
					}
				}
			}
			SemAn.ofType.put(funDefn, RetType);
		} else {
			RetType = SemAn.ofType.get(funDefn);
			int count = FuncTypeStack.size();
			if (funDefn.stmt != null) FuncTypeStack.push(RetType);
			if (funDefn.defns != null) funDefn.defns.accept(this, null);
			if (funDefn.stmt != null) funDefn.stmt.accept(this, null);
			if (count != FuncTypeStack.size()) FuncTypeStack.pop();
		}
		return RetType;
	}

	@Override
	public SemType visit(AstNodes<? extends AstNode> nodes, Context arg) {
		SemType semType = null;
		if(nodes.get(0) instanceof AstRecType.AstCmpDefn){
			Vector<SemType> CmpTypes = CmpVecStack.peek();
			for(AstNode cmp : nodes){
				SemType CmpType = cmp.accept(this, null);
				if(CmpType instanceof SemVoidType){
					throw new Report.Error(cmp, ((AstRecType.AstCmpDefn) cmp).name +
							"Component cannot be type void.");
				}
				CmpTypes.add(CmpType);
			}
			return semType;
		}
		for(Context c : new Context[]{Context.FIRST, Context.SECOND, Context.THIRD}) {
			for(AstNode n : nodes){
				if(n instanceof AstDefn){
					if (n instanceof AstTypDefn){
						n.accept(this, c);
					} else if (c == Context.SECOND && n instanceof AstFunDefn) {
						n.accept(this, c);
					} else if (c == Context.THIRD) {
						n.accept(this, c);
					}
				} else if (c == Context.THIRD) {
					n.accept(this, null);
				}
			}
		}
		return semType;
	}
}