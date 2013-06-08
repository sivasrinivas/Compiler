package edu.ufl.cise.cop5555.sp12;


import java.util.*;
import edu.ufl.cise.cop5555.sp12.TokenStream.*;
import edu.ufl.cise.cop5555.sp12.ast.*;
import edu.ufl.cise.cop5555.sp12.context.*;

public class Parser {

	public TokenStream stream;
	public TokenStreamIterator iterator;
	Token t=null;
	Token prevToken=null;
	Token errToken=null;
	boolean err=false;
	List<Kind> predictList=null;

	//predict lists
	List<Kind> predictSimpleType=null;
	List<Kind> predictCompoundType=null;
	List<Kind> predictType=null;
	List<Kind> predictDeclaration=null;	
	List<Kind> predictCommand=null;
	List<Kind> predictExpression=null;
	List<Kind> predictPairList=null;
	List<Kind> predictLValue=null;
	List<Kind> predictPair=null;
	List<Kind> predictRelOp=null;		
	List<Kind> predictWeakOp=null;
	List<Kind> predictStrongOp=null;		

	public Parser(TokenStream stream)
	{
		this.stream=stream;
		iterator=stream.iterator();
		if(iterator.hasNext())
			t=iterator.next();
		setPredictLists();
	}

	public AST parse() throws SyntaxException{
		
		AST ast=program();  //method corresponding to start symbol
		
		return ast;
	}

	private Program program() throws SyntaxException{
		// TODO IMPLEMENT THIS	
		
		match(Kind.PROG);
		match(Kind.IDENTIFIER);
		Token x=prevToken;
		Block block=block();
		match(Kind.GORP);
		match(Kind.EOF);

		Program p=new Program(x,block);
		return p;
	}

	public Block block() throws SyntaxException{
		DecOrCommand docObj=null;
		List<DecOrCommand> docList=new ArrayList<DecOrCommand>();

		while(isKind(predictDeclaration)||isKind(predictCommand)){
			
			if(isKind(Kind.SEMI)){
				consume();
			}
			else if(isKind(predictDeclaration)){
				docObj=declaration();
				match(Kind.SEMI);
				docList.add(docObj);
			}
			else if(isKind(predictCommand)){
				docObj=command();
				match(Kind.SEMI);
				docList.add(docObj);
			}
		}
		Block block=new Block(docList);
		return block;
	}

	public Declaration declaration() throws SyntaxException{
		Type type=type();
		match(Kind.IDENTIFIER);
		Token x=prevToken;
		
		Declaration dec=new Declaration(type,x);
		return dec;
	}

	public Type type() throws SyntaxException{
		Type type=null;
		if(isKind(predictSimpleType))
			type=simpleType();
		else if(isKind(predictCompoundType))
			type=compoundType();
		//else error();

		return type;
	}

	public SimpleType simpleType() throws SyntaxException{
		SimpleType simple=null;
		if(isKind(Kind.INT)||isKind(Kind.BOOLEAN)||isKind(Kind.STRING)){
			consume();
			simple=new SimpleType(prevToken.kind);
		}
		else error("Expected: INT or BOOLEAN or STRING");
		return simple;
		
	}

	public Type compoundType() throws SyntaxException{
		SimpleType simple=null;
		Type type=null;
		
		match(Kind.MAP);
		match(Kind.LEFT_SQUARE);
		simple=simpleType();
		match(Kind.COMMA);
		type=type();
		match(Kind.RIGHT_SQUARE);

		return new CompoundType(simple,type);
	}

	
	public Command command() throws SyntaxException{
		
		Expression expr=null;
		PairList pairList=null;
		LValue lValue=null;
		Block block=null;
		
		if(isKind(predictLValue)){
			lValue=lValue();
			match(Kind.ASSIGN);
			if(isKind(predictExpression)){
				expr=expression();
				return new AssignExprCommand(lValue,expr);
			}
			else if(isKind(predictPairList)){
				pairList=pairList();
				return new AssignPairListCommand(lValue,pairList);
			}
			//else error();

		}
		else if(isKind(Kind.PRINT)){
			consume();
			expr=expression();
			return new PrintCommand(expr);
		}
		else if(isKind(Kind.PRINTLN)){
			consume();
			expr=expression();
			return new PrintlnCommand(expr);
		}
		else if(isKind(Kind.DO)){
			consume();
			if(isKind(Kind.LEFT_PAREN)){
				match(Kind.LEFT_PAREN);
				expr=expression();
				match(Kind.RIGHT_PAREN);
				block=block();
				match(Kind.OD);
				return new DoCommand(expr,block);
			}
			else if(isKind(predictLValue)){
				lValue=lValue();
				match(Kind.COLON);
				match(Kind.LEFT_SQUARE);
				match(Kind.IDENTIFIER);
				Token x=prevToken;
				match(Kind.COMMA);
				match(Kind.IDENTIFIER);
				Token y=prevToken;
				match(Kind.RIGHT_SQUARE);
				block=block();
				match(Kind.OD);
				return new DoEachCommand(lValue,x,y,block);
			}
			else error("Expected : DO or DoEachCommand but not");
			return null;
		}
		else if(isKind(Kind.IF)){
			consume();
			match(Kind.LEFT_PAREN);
			expr=expression();
			match(Kind.RIGHT_PAREN);
			Block b1=block();
			if(isKind(Kind.ELSE)){
				consume();
				Block b2=block();
				match(Kind.FI);
				return new IfElseCommand(expr,b1,b2);
			}
			else if(isKind(Kind.FI)){
				match(Kind.FI);
				return new IfCommand(expr,b1);
			}
			else error("Expected : IF or IF Else command");
				return null;
		}
		else error("Expected: IDENTIFIER or PRINT or PRINTLLN or DO or IF");
		return null;
	}

	public LValue lValue() throws SyntaxException{
		Expression expr=null;
		
		match(Kind.IDENTIFIER);
		Token x=prevToken;
		if(isKind(Kind.LEFT_SQUARE)){
			consume();
			expr=expression();
			match(Kind.RIGHT_SQUARE);
			return new ExprLValue(x,expr);
		}
		else
			return new SimpleLValue(x);
	}

	public Pair pair() throws SyntaxException{
		Expression expr1=null;
		Expression expr2=null;
		
		match(Kind.LEFT_SQUARE);
		expr1=expression();
		match(Kind.COMMA);
		expr2=expression();
		match(Kind.RIGHT_SQUARE);
		
		return new Pair(expr1,expr2);
	}

	public PairList pairList() throws SyntaxException{
		List<Pair> pair=new ArrayList<Pair>();
		match(Kind.LEFT_BRACE);
		if(isKind(predictPair)){
			pair.add(pair());
			while(isKind(Kind.COMMA)){
				match(Kind.COMMA);
				pair.add(pair());
			}
		}
		match(Kind.RIGHT_BRACE);
		return new PairList(pair);
	}

	public Expression expression() throws SyntaxException{
		Expression expr1=null;
		Expression expr2=null;
		
		expr1=term();
		while(isKind(predictRelOp)){
			Token x=t;
			relOp();
			expr2=term();
			expr1= new BinaryOpExpression(expr1,x.kind,expr2);
		}
		return expr1;
	}

	public Expression term() throws SyntaxException{
		Expression expr1=null;
		Expression expr2=null;
		
		expr1=elem();
		while(isKind(predictWeakOp)){
			Token x=t;
			weakOp();
			expr2=elem();
			expr1=new BinaryOpExpression(expr1,x.kind,expr2);
		}
		return expr1;
	}

	public Expression elem() throws SyntaxException{
		Expression expr1=null;
		Expression expr2=null;
		expr1=factor();
		while(isKind(predictStrongOp)){
			Token x=t;
			strongOp();
			expr2=factor();
			expr1=new BinaryOpExpression(expr1,x.kind,expr2);
		}
		return expr1;
	}

	public Expression factor() throws SyntaxException{
		
		LValue lValue=null;
		Expression expr=null;
		Expression factor=null;
		
		if(isKind(predictLValue)){
			lValue=lValue();
			return new LValueExpression(lValue);
		}
		else if(isKind(Kind.INTEGER_LITERAL)||isKind(Kind.BOOLEAN_LITERAL)||isKind(Kind.STRING_LITERAL)){
			consume();
			Token x=prevToken;
			if(x.kind==Kind.INTEGER_LITERAL)
				return new IntegerLiteralExpression(x);
			else if(x.kind==Kind.BOOLEAN_LITERAL)
				return new BooleanLiteralExpression(x);
			else if(x.kind==Kind.STRING_LITERAL)
				return new StringLiteralExpression(x);
		}
		else if(isKind(Kind.LEFT_PAREN)){
			consume();
			expr=expression();
			match(Kind.RIGHT_PAREN);
			return expr;
		}
		else if(isKind(Kind.NOT)||isKind(Kind.MINUS)){
			consume();
			Token x=prevToken;
			factor=factor();
			return new UnaryOpExpression(x.kind,factor);
		}
		else error("Expected: IDENTIFIER or INTEGER_LITERAL or BOOLEAN_LITERAL or STRING_LITERAL LEFT_PAREN or NOT or MINUS");
		return null;
	}

	public void relOp() throws SyntaxException{
		if(isKind(Kind.OR)||isKind(Kind.AND)||isKind(Kind.EQUALS)||isKind(Kind.NOT_EQUALS)||isKind(Kind.LESS_THAN)||isKind(Kind.GREATER_THAN)||isKind(Kind.AT_LEAST)||isKind(Kind.AT_MOST)){
			consume();
		}
			
		else error("Expected: OR or AND or EQUALS or NOT_EQUALS or LESS_THAN or GREATER_THAN or AT_MOST or AT_LEAST");
	}

	public void weakOp() throws SyntaxException{
		if(isKind(Kind.PLUS)|isKind(Kind.MINUS))
			consume();
		else error("Expected: PLUS or MINUS");
	}

	public void strongOp() throws SyntaxException{
		if(isKind(Kind.TIMES)|isKind(Kind.DIVIDE))
			consume();
		else error("Expected: TIMES or DIVIDE");
	}

	public void consume(){
		//				System.out.println("Consumed:"+t);
		if(iterator.hasNext()){
			prevToken=t;
			t=iterator.next();
		}
			
		
	}

/*	public void match(Kind kind) throws SyntaxException{
		if(kind==Kind.EOF && err){
			new SyntaxException(errToken,"Unexpected EOF Token Occured");
		}
		else if(isKind(kind)){
			consume();
		}
		else {
			try{
				if(!err){
					errToken=t;
					err=true;
				}
				throw new SyntaxException(errToken,"Expected Kind: "+kind+"----->Actual Kind :"+t.kind);
			}
			catch(SyntaxException e){
				e.printStackTrace();
				if(kind != Kind.SEMI){
					while(!isKind(kind)&&!isKind(Kind.EOF))  //Need to check again that SEMI condition - &&!isKind(Kind.SEMI)
					consume();
					if(isKind(Kind.EOF))
						throw new SyntaxException(errToken,"Expected Kind: "+kind+"----->Actual Kind :"+t.kind);
				}
			}

		}

	}*/
	
	public void match(Kind kind) throws SyntaxException{
		if(isKind(kind))
			consume();
		else
			throw new SyntaxException(t,"Expected kind: "+kind+" --- Actual kind :"+t.kind);
	}

	public void error(String msg) throws SyntaxException{
		throw new SyntaxException(t,msg);
	}

	public boolean isKind(Kind kind){
		return kind==t.kind;
	}

	public boolean isKind(List<Kind> predictList){
		return predictList.contains(t.kind);
	}

	public void setPredictLists(){

		predictSimpleType=new ArrayList<Kind>();
		predictSimpleType.add(Kind.INT);
		predictSimpleType.add(Kind.BOOLEAN);
		predictSimpleType.add(Kind.STRING);

		predictCompoundType=new ArrayList<Kind>();
		predictCompoundType.add(Kind.MAP);

		predictType=new ArrayList<Kind>();
		predictType.add(Kind.MAP);
		predictType.add(Kind.INT);
		predictType.add(Kind.BOOLEAN);
		predictType.add(Kind.STRING);

		predictDeclaration=new ArrayList<Kind>();	
		predictDeclaration.add(Kind.INT);
		predictDeclaration.add(Kind.BOOLEAN);
		predictDeclaration.add(Kind.STRING);
		predictDeclaration.add(Kind.MAP);

		predictCommand=new ArrayList<Kind>();
		predictCommand.add(Kind.IDENTIFIER);
		predictCommand.add(Kind.PRINT);
		predictCommand.add(Kind.PRINTLN);
		predictCommand.add(Kind.DO);
		predictCommand.add(Kind.IF);
		predictCommand.add(Kind.SEMI); //follow of command

		predictExpression=new ArrayList<Kind>();
		predictExpression.add(Kind.IDENTIFIER);
		predictExpression.add(Kind.INTEGER_LITERAL);
		predictExpression.add(Kind.BOOLEAN_LITERAL);
		predictExpression.add(Kind.STRING_LITERAL);
		predictExpression.add(Kind.LEFT_PAREN);
		predictExpression.add(Kind.NOT);
		predictExpression.add(Kind.MINUS);

		predictPairList=new ArrayList<Kind>();
		predictPairList.add(Kind.LEFT_BRACE);

		predictLValue=new ArrayList<Kind>();
		predictLValue.add(Kind.IDENTIFIER);

		predictPair=new ArrayList<Kind>();
		predictPair.add(Kind.LEFT_SQUARE);

		predictRelOp=new ArrayList<Kind>();		
		predictRelOp.add(Kind.OR);
		predictRelOp.add(Kind.AND);
		predictRelOp.add(Kind.EQUALS);
		predictRelOp.add(Kind.NOT_EQUALS);
		predictRelOp.add(Kind.LESS_THAN);
		predictRelOp.add(Kind.GREATER_THAN);
		predictRelOp.add(Kind.AT_MOST);
		predictRelOp.add(Kind.AT_LEAST);

		predictWeakOp=new ArrayList<Kind>();
		predictWeakOp.add(Kind.PLUS);
		predictWeakOp.add(Kind.MINUS);

		predictStrongOp=new ArrayList<Kind>();		
		predictStrongOp.add(Kind.TIMES);
		predictStrongOp.add(Kind.DIVIDE);
	}


}
