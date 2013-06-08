package edu.ufl.cise.cop5555.sp12.codegen;

import java.util.*;
import org.objectweb.asm.*;

import edu.ufl.cise.cop5555.sp12.Kind;
import edu.ufl.cise.cop5555.sp12.ast.*;
import edu.ufl.cise.cop5555.sp12.ast.Type;
import edu.ufl.cise.cop5555.sp12.context.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	int counter=0;
	String className;
	SymbolTable symbol=null;
	ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
	FieldVisitor fv;
	MethodVisitor mv;
	AnnotationVisitor av0;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		className = program.ident.getText();
		symbol=new SymbolTable();
		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		//set label on first instruction of main method
		Label lstart = new Label();
		mv.visitLabel(lstart);
		//visit block to generate code and field declarations
		program.block.visit(this,null);
		//add return instruction
		mv.visitInsn(RETURN);
		Label lend= new Label();
		mv.visitLabel(lend);
		//visit local variable--the only one in our project is the String[] argument to the main method
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, lstart, lend, 0);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
		cw.visitEnd();
		//convert class file to byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		//visit children
		for (DecOrCommand cd : block.decOrCommands) {
			cd.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		declaration.type.visit(this, declaration.ident.getText());

		return null;
	}

	@Override
	public Object visitSimpleType(SimpleType simpleType, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		String identifier=(String)arg;
		if(identifier!=null){
			switch(simpleType.type){
			case INT:
				Integer initValue=new Integer(0);
				fv=cw.visitField(ACC_STATIC, identifier, "I", null, initValue);
				break;

			case STRING:
				String initString=new String("");
				fv=cw.visitField(ACC_STATIC, identifier, "Ljava/lang/String;", null, initString);
				break;

			case BOOLEAN:
				boolean init=false;
				fv=cw.visitField(ACC_STATIC, identifier, "Z", null, init);
				break;
			}

			fv.visitEnd();
		}
		else{
			String fieldType=null;
			switch(simpleType.type){
			case INT:
				fieldType="Ljava/lang/Integer";
				break;

			case STRING:
				fieldType="Ljava/lang/String";
				break;

			case BOOLEAN:
				fieldType="Ljava/lang/Boolean";
				break;
			}
			return fieldType;
		}
		return null;
	}

	@Override
	public Object visitCompoundType(CompoundType compoundType, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		String identifier=(String)arg;
		String mapName=null;
		if(identifier!=null){
			String keyString=(String)compoundType.keyType.visit(this, null);
			String valString=(String)compoundType.valType.visit(this, null);
			mapName="Ljava/util/HashMap<"+keyString+valString+">;";
			fv=cw.visitField(ACC_STATIC, identifier, "Ljava/util/HashMap;",mapName,null);
			fv.visitEnd();
			mv.visitTypeInsn(NEW,"java/util/HashMap");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V");
			mv.visitFieldInsn(PUTSTATIC, className, identifier, "Ljava/util/HashMap;");
		}
		else{
			String keyString=(String)compoundType.keyType.visit(this, arg);
			String valString=(String)compoundType.valType.visit(this, arg);
			mapName="Ljava/util/HashMap<"+keyString+valString+">;";
			return mapName;
		}
		return null;
	}

	@Override
	public Object visitAssignExprCommand(AssignExprCommand assignExprCommand,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		assignExprCommand.expression.visit(this, arg);
		if(assignExprCommand.lValue instanceof ExprLValue)
			box(mv,assignExprCommand.expression.type);
		assignExprCommand.lValue.visit(this, "PUT");

		//		assignExprCommand.type=assignExprCommand.expression.type;
		return null;
	}

	@Override
	public Object visitAssignPairListCommand( AssignPairListCommand assignPairListCommand, Object arg) throws Exception {
		// TODO Auto-generated method stub
		assignPairListCommand.pairList.visit(this, arg);
		assignPairListCommand.lValue.visit(this, "PUT");
		return null;
	}

	@Override
	public Object visitPrintCommand(PrintCommand printCommand, Object arg)
			throws Exception {
		//TODO Fix this to work with other types
		String fieldType = null;

		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		printCommand.expression.visit(this,arg);

		if(printCommand.expression.type instanceof SimpleType){
			SimpleType type=(SimpleType) printCommand.expression.type;

			switch(type.type){
			case INT:
				fieldType="(I)V";
				break;
			case STRING:
				fieldType="(Ljava/lang/String;)V";
				break;
			case BOOLEAN:
				fieldType="(Z)V";
				break;
			default:
				System.out.println("Error in print command");
			}
		}
		else if(printCommand.expression.type instanceof CompoundType){
			fieldType="(Ljava/lang/Object;)V";
		}
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", fieldType);

		return null;
	}

	@Override
	public Object visitPrintlnCommand(PrintlnCommand printlnCommand, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		String fieldType = null;
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		printlnCommand.expression.visit(this,arg);
		
		if(printlnCommand.expression.type instanceof SimpleType){
			SimpleType type=(SimpleType) printlnCommand.expression.type;

			switch(type.type){
			case INT:
				fieldType="(I)V";
				break;
			case STRING:
				fieldType="(Ljava/lang/String;)V";
				break;
			case BOOLEAN:
				fieldType="(Z)V";
				break;
			default:
				System.out.println("Error in print command");
			}
		}
		else if(printlnCommand.expression.type instanceof CompoundType){
			fieldType="(Ljava/lang/Object;)V";
		}

		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", fieldType);

		return null;
	}

	@Override
	public Object visitDoCommand(DoCommand doCommand, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Label l1=new Label();
		mv.visitJumpInsn(GOTO,l1);

		Label l2=new Label();
		mv.visitLabel(l2);
		doCommand.block.visit(this, arg);

		mv.visitLabel(l1);
		doCommand.expression.visit(this, arg);
		mv.visitJumpInsn(IFNE, l2);
		return null;
	}

	@Override
	public Object visitDoEachCommand(DoEachCommand doEachCommand, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
	
		doEachCommand.lValue.visit(this, "GET");
		Type lType=doEachCommand.lValue.type;
		CompoundType cType=(CompoundType) lType;
		
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "entrySet", "()Ljava/util/Set;");
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;");
		
		mv.visitVarInsn(ASTORE, 1);
		Label l1=new Label();
		mv.visitJumpInsn(GOTO, l1);
		Label l2=new Label();
		mv.visitLabel(l2);
		mv.visitFrame(Opcodes.F_FULL, 2, new Object[] {Opcodes.TOP, "java/util/Iterator"}, 0, new Object[] {});
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");
		mv.visitVarInsn(ASTORE, 0);
		
		Label l3=new Label();
		mv.visitLabel(l3);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;");
//		CheckCast(cType.keyType);
		
		SimpleLValue kType=new SimpleLValue(doEachCommand.key);
		kType.type=cType.keyType;
//		System.out.println("kType");
		unbox(mv,cType.keyType);
		visitSimpleLValue(kType,"PUT");
		
		Label l4=new Label();
		mv.visitLabel(l4);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;");
//		CheckCast(cType.valType);
		unbox(mv,cType.valType);
		SimpleLValue vType=new SimpleLValue(doEachCommand.val);
		vType.type=cType.valType;
//		System.out.println("vType");
		visitSimpleLValue(vType,"PUT");
		doEachCommand.block.visit(this, arg);
		mv.visitLabel(l1);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
		mv.visitJumpInsn(IFNE, l2);
		
		return null;
	}

	@Override
	public Object visitIfCommand(IfCommand ifCommand, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		ifCommand.expression.visit(this, arg);
		Label l1=new Label();
		mv.visitJumpInsn(IFEQ, l1);
		ifCommand.block.visit(this, arg);
		mv.visitLabel(l1);

		return null;
	}

	@Override
	public Object visitIfElseCommand(IfElseCommand ifElseCommand, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		ifElseCommand.expression.visit(this, arg);
		Label l1=new Label();
		mv.visitJumpInsn(IFEQ, l1);
		ifElseCommand.ifBlock.visit(this, arg);
		Label l2=new Label();
		mv.visitJumpInsn(GOTO, l2);
		mv.visitLabel(l1);
		ifElseCommand.elseBlock.visit(this, arg);
		mv.visitLabel(l2);

		return null;
	}

	@Override
	public Object visitSimpleLValue(SimpleLValue simpleLValue, Object arg)
			throws Exception {
		// TODO Auto-generated method stub

		String ident=simpleLValue.identifier.getText();
		String act=(String) arg;
		String fieldType = null;
		if(simpleLValue.type instanceof SimpleType){
			SimpleType type=(SimpleType) simpleLValue.type;

			if(type.type==Kind.INT)
				fieldType="I";
			else if(type.type==Kind.STRING)
				fieldType="Ljava/lang/String;";
			else if(type.type==Kind.BOOLEAN)
				fieldType="Z";
		}
		else if(simpleLValue.type instanceof CompoundType){
			fieldType="Ljava/util/HashMap;";
		}
		
		if(act.equals("PUT"))
			mv.visitFieldInsn(PUTSTATIC, className, ident, fieldType);
		else if(act.equals("GET"))
			mv.visitFieldInsn(GETSTATIC, className, ident, fieldType);
		return null;
	}

	@Override
	public Object visitExprLValue(ExprLValue exprLValue, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		String identifier=exprLValue.identifier.getText();
		String action=(String) arg;

		if(action.equals("PUT")){
			mv.visitFieldInsn(GETSTATIC, className, identifier, "Ljava/util/HashMap;");
			mv.visitInsn(SWAP);
			exprLValue.expression.visit(this, arg);
			box(mv,exprLValue.expression.type);
			mv.visitInsn(SWAP);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
		}
		else if(action.equals("GET")){
			mv.visitFieldInsn(GETSTATIC, className, identifier, "Ljava/util/HashMap;");
			exprLValue.expression.visit(this, arg);
			box(mv,exprLValue.type);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "get", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
			unbox(mv,exprLValue.type);
		}
		 
		return null;
	}

	@Override
	public Object visitPair(Pair pair, Object arg) throws Exception {
		// TODO Auto-generated method stub
		pair.expression0.visit(this, arg);
		box(mv,pair.expression0.type);
		pair.expression1.visit(this, arg);
		box(mv,pair.expression1.type);
		return null;
	}

	@Override
	public Object visitPairList(PairList pairList, Object arg) throws Exception {
		// TODO Auto-generated method stub
		List<Pair> pairlist=pairList.pairs;
		SimpleType sType1=(SimpleType) pairlist.get(0).expression0.type;
		SimpleType sType2=(SimpleType) pairlist.get(0).expression1.type;
		
		CompoundType cType=new CompoundType(sType1, sType2);
		visitCompoundType(cType, "pairListHashMap"+counter);
		
		for(Pair p: pairlist){
			mv.visitFieldInsn(GETSTATIC, className, "pairListHashMap"+counter, "Ljava/util/HashMap;");
			p.visit(this, arg);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
		}
		mv.visitFieldInsn(GETSTATIC, className, "pairListHashMap"+counter, "Ljava/util/HashMap;");
		counter++;
		
		return null;
	}

	@Override
	public Object visitLValueExpression(LValueExpression lValueExpression,
			Object arg) throws Exception {
		// TODO Auto-generated method stub

		lValueExpression.lValue.visit(this, "GET");
		//		lValueExpression.type=lValueExpression.lValue.type;
		return null;
	}

	@Override
	public Object visitIntegerLiteralExpression(
			IntegerLiteralExpression integerLiteralExpression, Object arg)
					throws Exception {
		//gen code to leave value of literal on top of stack

		mv.visitLdcInsn(integerLiteralExpression.integerLiteral.getIntVal());
		//		integerLiteralExpression.type= new SimpleType(Kind.INT);
		return null;
	}

	@Override
	public Object visitBooleanLiteralExpression(
			BooleanLiteralExpression booleanLiteralExpression, Object arg)
					throws Exception {
		// TODO Auto-generated method stub
		String str=booleanLiteralExpression.booleanLiteral.getText();
		if(str.equals("true"))
			mv.visitInsn(ICONST_1);
		else
			mv.visitInsn(ICONST_0);
		//		booleanLiteralExpression.type= new SimpleType(Kind.BOOLEAN);
		return null;
	}

	@Override
	public Object visitStringLiteralExpression(
			StringLiteralExpression stringLiteralExpression, Object arg)
					throws Exception {
		// TODO Auto-generated method stub
		mv.visitLdcInsn(stringLiteralExpression.stringLiteral.getText());
		//		stringLiteralExpression.type= new SimpleType(Kind.STRING);
		return null;
	}

	@Override
	public Object visitUnaryOpExpression(UnaryOpExpression unaryOpExpression,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		unaryOpExpression.expression.visit(this, arg);
		switch(unaryOpExpression.op){
		case NOT:
			Label l1 = new Label();
			mv.visitJumpInsn(IFEQ, l1);
			mv.visitInsn(ICONST_0);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(l2);
			//			unaryOpExpression.type=new SimpleType(Kind.BOOLEAN);
			break;
		case MINUS:
			mv.visitInsn(INEG);
			//			unaryOpExpression.type=new SimpleType(Kind.INT);
			break;
		default:
			throw new Exception("Invalid operation type in unary op expression");
		}
		return null;
	}

	@Override
	public Object visitBinaryOpExpression(
			BinaryOpExpression binaryOpExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Type expr0Type= binaryOpExpression.expression0.type;
		Type expr1Type= binaryOpExpression.expression1.type;

		SimpleType intType= new SimpleType(Kind.INT);
		SimpleType boolType= new SimpleType(Kind.BOOLEAN);
		SimpleType stringType= new SimpleType(Kind.STRING);
		Type binOpType=null;
		String fieldType="";

		switch(binaryOpExpression.op){
		case PLUS:
			if((expr0Type instanceof SimpleType && expr1Type instanceof SimpleType)){
				SimpleType st0=(SimpleType)expr0Type;
				SimpleType st1=(SimpleType)expr1Type;

				if(st0.type==Kind.STRING || st1.type==Kind.STRING ){

					mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
					mv.visitInsn(DUP);
					binaryOpExpression.expression0.visit(this, arg);
					if(st0.type==Kind.STRING)
						fieldType="(Ljava/lang/Object;)";
					else if(st0.type==Kind.INT)
						fieldType="(I)";
					else if(st0.type==Kind.BOOLEAN)
						fieldType="(Z)";

					mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", fieldType+"Ljava/lang/String;");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");

					binaryOpExpression.expression1.visit(this, arg);
					if(st1.type==Kind.STRING)
						fieldType="(Ljava/lang/String;)";
					else if(st1.type==Kind.INT)
						fieldType="(I)";
					else if(st1.type==Kind.BOOLEAN)
						fieldType="(Z)";

					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", fieldType+"Ljava/lang/StringBuilder;");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

					binOpType=stringType;
				}

				else if(st0.type==Kind.BOOLEAN || st1.type==Kind.BOOLEAN){
					throw new ContextException(binaryOpExpression,"PLUS is not defined on boolean");
				}
				else{
					binaryOpExpression.expression0.visit(this, arg);
					binaryOpExpression.expression1.visit(this, arg);
					mv.visitInsn(IADD);
					binOpType=intType;
				}
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				binaryOpExpression.expression0.visit(this, arg);
				binaryOpExpression.expression1.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"plus",Runtime.binopDescriptor);
			}

			break;

		case MINUS:
			binaryOpExpression.expression0.visit(this, arg);
			binaryOpExpression.expression1.visit(this, arg);
			
			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				mv.visitInsn(ISUB);
				binOpType=intType;
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"minus",Runtime.binopDescriptor);
			}
			break;

		case TIMES:
			binaryOpExpression.expression0.visit(this, arg);
			binaryOpExpression.expression1.visit(this, arg);
			
			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				mv.visitInsn(IMUL);
				binOpType=intType;
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"times",Runtime.binopDescriptor);
			}
			break;

		case DIVIDE:
			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				binaryOpExpression.expression0.visit(this, arg);
				binaryOpExpression.expression1.visit(this, arg);
				mv.visitInsn(IDIV);
				binOpType=intType;
			}
			break;
		case AND:
			if(expr0Type.equals(boolType)&&expr1Type.equals(boolType)){
				binaryOpExpression.expression0.visit(this, arg);
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1);
				binaryOpExpression.expression1.visit(this, arg);
				mv.visitJumpInsn(IFEQ, l1);
				mv.visitInsn(ICONST_1);
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);

				mv.visitInsn(ICONST_0);
				mv.visitLabel(l2);
				binOpType=boolType;
			}
			break;
		case OR:
			if(expr0Type.equals(boolType)&&expr1Type.equals(boolType)){
				binaryOpExpression.expression0.visit(this, arg);
				Label l1 = new Label();
				mv.visitJumpInsn(IFNE, l1);
				binaryOpExpression.expression1.visit(this, arg);
				mv.visitJumpInsn(IFNE, l1);
				mv.visitInsn(ICONST_0);
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);

				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
				binOpType=boolType;
			}
			break;

		case EQUALS:
			if(expr0Type instanceof SimpleType && expr1Type instanceof SimpleType){
				SimpleType st0=(SimpleType)expr0Type;
				if(st0.type!=Kind.STRING)
				{
					binaryOpExpression.expression0.visit(this, arg);
					binaryOpExpression.expression1.visit(this, arg);
					Label l3 = new Label();
					mv.visitJumpInsn(IF_ICMPNE, l3);
					mv.visitInsn(ICONST_1);

					Label l4 = new Label();
					mv.visitJumpInsn(GOTO, l4);
					mv.visitLabel(l3);
					mv.visitInsn(ICONST_0);
					mv.visitLabel(l4);
				}
				else
				{
					binaryOpExpression.expression0.visit(this, arg);
					binaryOpExpression.expression1.visit(this, arg);
					Label l3 = new Label();
					mv.visitJumpInsn(IF_ACMPNE, l3);
					mv.visitInsn(ICONST_1);

					Label l4 = new Label();
					mv.visitJumpInsn(GOTO, l4);
					mv.visitLabel(l3);
					mv.visitInsn(ICONST_0);
					mv.visitLabel(l4);
				}
				binOpType=boolType;
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				binaryOpExpression.expression0.visit(this, arg);
				binaryOpExpression.expression1.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"equals",Runtime.relopDescriptor);
			}
			break;

		case NOT_EQUALS:
			binaryOpExpression.expression0.visit(this, arg);
			binaryOpExpression.expression1.visit(this, arg);
			
			if(expr0Type instanceof SimpleType&&expr1Type instanceof SimpleType){
				SimpleType st0=(SimpleType)expr0Type;

				if(st0.type!=Kind.STRING)
				{
					Label l1 = new Label();
					mv.visitJumpInsn(IF_ICMPEQ, l1);
					mv.visitInsn(ICONST_1);

					Label l2 = new Label();
					mv.visitJumpInsn(GOTO, l2);
					mv.visitLabel(l1);
					mv.visitInsn(ICONST_0);
					mv.visitLabel(l2);
				}
				else
				{
					Label l3 = new Label();
					mv.visitJumpInsn(IF_ACMPEQ, l3);
					mv.visitInsn(ICONST_1);

					Label l4 = new Label();
					mv.visitJumpInsn(GOTO, l4);
					mv.visitLabel(l3);
					mv.visitInsn(ICONST_0);
					mv.visitLabel(l4);
				}
				binOpType=boolType;
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"not_equals",Runtime.relopDescriptor);
			}
			break;

		case LESS_THAN:
			binaryOpExpression.expression0.visit(this, arg);
			binaryOpExpression.expression1.visit(this, arg);

			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPLT,l1);
				mv.visitInsn(ICONST_0);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
			}
			else if(expr0Type.equals(boolType)&&expr1Type.equals(boolType)){
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPLT,l1);
				mv.visitInsn(ICONST_0);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"less_than",Runtime.relopDescriptor);
			}
			binOpType=boolType;
			break;

		case GREATER_THAN:
			binaryOpExpression.expression0.visit(this, arg);
			binaryOpExpression.expression1.visit(this, arg);
			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPGT,l1);
				mv.visitInsn(ICONST_0);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
				binOpType=boolType;
			}

			else if(expr0Type.equals(boolType)&&expr1Type.equals(boolType)){
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPGT,l1);
				mv.visitInsn(ICONST_0);
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
				binOpType=boolType;
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"greater_than",Runtime.relopDescriptor);
			}
			break;

		case AT_MOST:
			
			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				binaryOpExpression.expression0.visit(this, arg);
				binaryOpExpression.expression1.visit(this, arg);
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPGT,l1);
				mv.visitInsn(ICONST_1);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(l2);
				binOpType=boolType;
			}

			else if(expr0Type.equals(boolType)&&expr1Type.equals(boolType)){
				binaryOpExpression.expression0.visit(this, arg);
				binaryOpExpression.expression1.visit(this, arg);
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPGT,l1);
				mv.visitInsn(ICONST_1);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(l2);
				binOpType=boolType;
			}

			else if(expr0Type.equals(stringType)&&expr1Type.equals(stringType)){
				binaryOpExpression.expression1.visit(this, arg);
				binaryOpExpression.expression0.visit(this, arg);
				
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
				binOpType=boolType;
			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				binaryOpExpression.expression0.visit(this, arg);
				binaryOpExpression.expression1.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"at_most",Runtime.relopDescriptor);
			}
			break;

		case AT_LEAST:
			binaryOpExpression.expression0.visit(this, arg);
			binaryOpExpression.expression1.visit(this, arg);
			if(expr0Type.equals(intType)&&expr1Type.equals(intType)){
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPLT,l1);
				mv.visitInsn(ICONST_1);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(l2);

			}

			else if(expr0Type.equals(boolType)&&expr1Type.equals(boolType)){
				Label l1=new Label();
				mv.visitJumpInsn(IF_ICMPLT,l1);
				mv.visitInsn(ICONST_1);

				Label l2 = new Label();
				mv.visitJumpInsn(GOTO,l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(l2);

			}
			else if(expr0Type instanceof CompoundType && expr1Type instanceof CompoundType){
				mv.visitMethodInsn(INVOKESTATIC,Runtime.className,"at_least",Runtime.relopDescriptor);
			}
			binOpType=boolType;
			break;

		}
		//		binaryOpExpression.type=binOpType;
		return null;
	}

	void box(MethodVisitor mv, Type t) {
		if (t.equals(new SimpleType(Kind.INT))) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
					"(I)Ljava/lang/Integer;");
		}
		if (t.equals(new SimpleType(Kind.BOOLEAN))) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
					"(Z)Ljava/lang/Boolean;");
		}
	}
	// if primitive type, generates code to unbox, also inserts checkcast
	// instructions for all types
	// if returned type is a HashMap that is null, instantiates the hashmap
	void unbox(MethodVisitor mv, Type t) {
		if (t.equals(new SimpleType(Kind.INT))) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue",
					"()I");
		}
		if (t.equals(new SimpleType(Kind.BOOLEAN))) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean",
					"booleanValue", "()Z");
		}
		if (t.equals(new SimpleType(Kind.STRING))) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/String");
		}
		if (t instanceof CompoundType) {
			Label ubl = new Label();
			mv.visitInsn(DUP);
			mv.visitJumpInsn(IFNONNULL, ubl);
			mv.visitInsn(POP);
			instantiateHashMap(mv);
			mv.visitLabel(ubl);
//			mv.visitTypeInsn(CHECKCAST, HASHMAPNAME);
		}
	}

	private void instantiateHashMap(MethodVisitor mv2) {
		// TODO Auto-generated method stub
		
	}

}
