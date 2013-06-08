package edu.ufl.cise.cop5555.sp12.context;

import java.util.List;

import edu.ufl.cise.cop5555.sp12.ast.*;
import edu.ufl.cise.cop5555.sp12.Kind;

public class ContextCheckVisitor implements ASTVisitor {
	public String progName;
	SymbolTable symbol=null;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		progName=program.ident.getText();
		symbol=new SymbolTable();
		program.block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		symbol.enterScope();
		List<DecOrCommand> doc=block.decOrCommands;
		if(doc.isEmpty()){

		}
		else{
			for(DecOrCommand i : doc){
				i.visit(this, arg);
			}
		}
		symbol.exitScope();
		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg)	throws Exception {
		if(progName.equals(declaration.ident.getText())){
			throw new ContextException(declaration,"Program name used as Varialbe");
		}
		else{
			if(symbol.insert(declaration.ident.getText(), declaration)){
				declaration.type.visit(this, arg);
			}
			else
				throw new ContextException(declaration,"Identfier name is already used");
		}
		return null;
	}

	@Override
	public Object visitSimpleType(SimpleType simpleType, Object arg) throws Exception {
		if(simpleType.type==Kind.INT||simpleType.type==Kind.BOOLEAN||simpleType.type==Kind.STRING){
			return simpleType;
		}
		else{
			throw new ContextException(simpleType,"incomatible type in Simple Type");
		}
	}

	@Override
	public Object visitCompoundType(CompoundType compoundType, Object arg) throws Exception {

		compoundType.keyType.visit(this, arg);
		compoundType.valType.visit(this, arg);
		return compoundType;
	}

	@Override
	public Object visitAssignExprCommand(AssignExprCommand assignExprCommand, Object arg) throws Exception {

		Type lhsType=(Type) assignExprCommand.lValue.visit(this, arg);
		Type exprType=(Type) assignExprCommand.expression.visit(this, arg);

		check(lhsType.equals(exprType),assignExprCommand,"incompatible types in Assignment Expression Command");
		return null;
	}


	@Override
	public Object visitAssignPairListCommand(AssignPairListCommand assignPairListCommand, Object arg) throws Exception {
		Type lhsType=(Type) assignPairListCommand.lValue.visit(this, arg);
		Type pairListType=(Type) assignPairListCommand.pairList.visit(this, arg);
		List<Pair> pList=assignPairListCommand.pairList.pairs;
		if(pList.isEmpty()){
			return lhsType;
		}
		else 
			check(lhsType.equals(pairListType),assignPairListCommand,"incompatible types in Assign PairList Command");
		return pairListType;
	}

	@Override
	public Object visitPrintCommand(PrintCommand printCommand, Object arg) throws Exception {
		printCommand.expression.visit(this, arg);
		return null;
	}

	@Override
	public Object visitPrintlnCommand(PrintlnCommand printlnCommand, Object arg) throws Exception {
		printlnCommand.expression.visit(this, arg);
		return null;
	}

	@Override
	public Object visitDoCommand(DoCommand doCommand, Object arg) throws Exception {
		Type exprType=(Type) doCommand.expression.visit(this, arg);
		SimpleType st=new SimpleType(Kind.BOOLEAN);
		check(exprType.equals(st),doCommand,"Incompatible type in DoCommand");
		doCommand.block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitDoEachCommand(DoEachCommand doEachCommand, Object arg) throws Exception {
		CompoundType lValueType=(CompoundType) doEachCommand.lValue.visit(this, arg);
		Type keyType=null;
		Type valType=null;
		if(lValueType instanceof CompoundType){
			keyType=symbol.lookup(doEachCommand.key.getText()).type;
			if(symbol.lookup(doEachCommand.val.getText())!=null){
				valType=symbol.lookup(doEachCommand.val.getText()).type;
			}
			else
				throw new ContextException(doEachCommand,"Undeclared variable") ;
			check((lValueType.keyType.equals(keyType)&&lValueType.valType.equals(valType)),doEachCommand,"incompatible types in DoEachCommand");
			doEachCommand.block.visit(this, arg);
		}
		else 
			throw new ContextException(doEachCommand,"incompatible types in Do Each Command") ;

		return null;
	}

	@Override
	public Object visitIfCommand(IfCommand ifCommand, Object arg) throws Exception {
		Type exprType=(Type) ifCommand.expression.visit(this, arg);
		SimpleType st=new SimpleType(Kind.BOOLEAN);
		check(exprType.equals(st),ifCommand,"Incompatible type in IF COmmand");
		ifCommand.block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitIfElseCommand(IfElseCommand ifElseCommand, Object arg) throws Exception {
		Type exprType=(Type) ifElseCommand.expression.visit(this, arg);
		SimpleType st=new SimpleType(Kind.BOOLEAN);
		check(exprType.equals(st),ifElseCommand,"Incompatible type in IF COmmand");
		ifElseCommand.ifBlock.visit(this, arg);
		ifElseCommand.elseBlock.visit(this, arg);
		return null;
	}

	@Override
	public Object visitSimpleLValue(SimpleLValue simpleLValue, Object arg) throws Exception {
		Declaration ident=symbol.lookup(simpleLValue.identifier.getText());
		if(ident!=null){
			simpleLValue.type=ident.type;
			return ident.type;
		}
		throw new ContextException(simpleLValue,"Declaration not found in Symbol table - Simple LValue");
	}

	@Override
	public Object visitExprLValue(ExprLValue exprLValue, Object arg) throws Exception {
		Declaration ident=symbol.lookup(exprLValue.identifier.getText());
		if(ident==null)
			throw new ContextException(exprLValue,"incompatible types in Expression LValue");
		else if(ident!=null){
			Type exprType=(Type) exprLValue.expression.visit(this, arg);
			check(exprType.equals(((CompoundType)ident.type).keyType),exprLValue,"Incompatible types ExprLValue");
		}
		exprLValue.type=((CompoundType)ident.type).valType;
		return exprLValue.type;
	}

	@Override
	public Object visitPair(Pair pair, Object arg) throws Exception {
		SimpleType type0=(SimpleType) pair.expression0.visit(this, arg);
		Type type1=(Type) pair.expression1.visit(this, arg);
		CompoundType ctype=new CompoundType(type0,type1);
		return ctype;
	}

	@Override
	public Object visitPairList(PairList pairList, Object arg) throws Exception {
		List <Pair> pList=pairList.pairs;
		if(pList.isEmpty())
			return null;
		else{
			Type type0=(Type)pList.get(0).visit(this, arg);
			for(Pair p : pList){
				Type type1=(Type) p.visit(this, arg);
				check(type1.equals(type0),pairList,"incompatible types in PairList");
			}
			return type0;
		}

	}

	@Override
	public Object visitLValueExpression(LValueExpression lValueExpression, Object arg) throws Exception {
		lValueExpression.type=(Type) lValueExpression.lValue.visit(this, arg);
		return lValueExpression.type;
	}

	@Override
	public Object visitIntegerLiteralExpression(IntegerLiteralExpression integerLiteralExpression, Object arg)	throws Exception {
		SimpleType st=new SimpleType(Kind.INT);
		//		SimpleType exprType=new SimpleType(integerLiteralExpression.integerLiteral.kind);
		//		check(st.equals(exprType),integerLiteralExpression,"Incompatible types in IntegerLiteralExpression");
		integerLiteralExpression.type=new SimpleType(Kind.INT);
		return integerLiteralExpression.type;
	}

	@Override
	public Object visitBooleanLiteralExpression( BooleanLiteralExpression booleanLiteralExpression, Object arg) throws Exception {
		SimpleType st=new SimpleType(Kind.BOOLEAN);
		//		SimpleType exprType=new SimpleType(booleanLiteralExpression.booleanLiteral.kind);
		//		check(st.equals(exprType),booleanLiteralExpression,"Incompatible types in IntegerLiteralExpression");
		booleanLiteralExpression.type=new SimpleType(Kind.BOOLEAN);
		return booleanLiteralExpression.type;
	}

	@Override
	public Object visitStringLiteralExpression( StringLiteralExpression stringLiteralExpression, Object arg) throws Exception {
		SimpleType st=new SimpleType(Kind.STRING);
		//		SimpleType exprType=new SimpleType(stringLiteralExpression.stringLiteral.kind);
		//		check(st.equals(exprType),stringLiteralExpression,"Incompatible types in IntegerLiteralExpression");
		stringLiteralExpression.type=new SimpleType(Kind.STRING);
		return stringLiteralExpression.type;
	}

	@Override
	public Object visitUnaryOpExpression(UnaryOpExpression unaryOpExpression, Object arg) throws Exception {
		check((unaryOpExpression.op==Kind.MINUS||unaryOpExpression.op==Kind.NOT),unaryOpExpression,"Incompatible operation in UnaryOpExpression");
		Type type=(Type)unaryOpExpression.expression.visit(this, arg);
		SimpleType st=null;
		if(unaryOpExpression.op==Kind.MINUS){
			st=new SimpleType(Kind.INT);
			if(!type.equals(st))
				throw new ContextException(unaryOpExpression,"incompatible types in unaryExpression");
			unaryOpExpression.type=type;
			return unaryOpExpression.type;
		}
		else if(unaryOpExpression.op==Kind.NOT){
			st=new SimpleType(Kind.BOOLEAN);
			if(!type.equals(st))
				throw new ContextException(unaryOpExpression,"incompatible types in unaryExpression");
			unaryOpExpression.type=type;
			return unaryOpExpression.type;
		}
		throw new ContextException(unaryOpExpression,"incompatible types in unaryExpression");

	}

	@Override
	public Object visitBinaryOpExpression(BinaryOpExpression binaryOpExpression, Object arg) throws Exception {
		Type expr0Type=(Type) binaryOpExpression.expression0.visit(this, arg);
		Type expr1Type=(Type) binaryOpExpression.expression1.visit(this, arg);
		Type intType=(Type) new SimpleType(Kind.INT);
		Type boolType=(Type) new SimpleType(Kind.BOOLEAN);
		Type stringType=(Type) new SimpleType(Kind.STRING);
		Type binOpType=null;
		boolean equals=false;
		if(binaryOpExpression.op==Kind.PLUS){
			if(expr0Type.equals(stringType)){
				if(expr1Type.equals(boolType)||expr1Type.equals(intType)){
					equals=true;
				}
			}
			else if(expr1Type.equals(stringType)){
				if(expr0Type.equals(boolType)||expr0Type.equals(intType)){
					equals=true;
				}
			}
		}
		if(equals!=true){
			equals=expr1Type.equals(expr0Type);
			check(equals,binaryOpExpression,"Incompatible types in Binary Op Expression");
		}
		switch(binaryOpExpression.op){
		case PLUS:	
			if(expr1Type.equals(stringType)||expr0Type.equals(stringType)){
				if(expr0Type.equals(intType)||expr0Type.equals(boolType)||expr0Type.equals(stringType))
					binOpType=stringType;
				else if(expr1Type.equals(intType)||expr1Type.equals(boolType)||expr1Type.equals(stringType))
					binOpType=stringType;
			}
			else if(expr0Type.equals(expr0Type)){
				if(!expr0Type.equals(boolType))
					binOpType=expr0Type;
			}			
			break;
		case MINUS:
		case TIMES:
			if(expr0Type.equals(expr1Type)){
				if(expr0Type.equals(intType))
					binOpType=expr0Type;
				else if(expr0Type instanceof CompoundType)
					binOpType=expr0Type;
			}				
			break;
		case DIVIDE:
			if(expr0Type.equals(expr1Type)){
				if(expr0Type.equals(intType))
					binOpType=expr0Type;
			}
			break;
		case EQUALS:	
		case NOT_EQUALS: 	
		case LESS_THAN:		
		case GREATER_THAN:		
		case AT_LEAST:		
		case AT_MOST:
			if(expr0Type.equals(expr1Type))
				binOpType=boolType;
			break;
		case OR:		
		case AND:
			if(expr0Type.equals(boolType)&&expr1Type.equals(boolType))
				binOpType=boolType;
			break;
		}
		if(binOpType!=null){
			binaryOpExpression.type=binOpType;
			return binOpType;
		}

		else
			throw new ContextException(binaryOpExpression,"incompatible types in Binary Op Expression");
	}

	private void check(boolean equals, AST obj,	String string) throws ContextException{
		if(equals==false) 
			throw new ContextException(obj, string);

	}

}
