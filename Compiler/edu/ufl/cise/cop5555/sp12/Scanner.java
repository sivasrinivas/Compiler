package edu.ufl.cise.cop5555.sp12;


import java.util.*;
import edu.ufl.cise.cop5555.sp12.TokenStream.Token;
import edu.ufl.cise.cop5555.sp12.TokenStream.TokenStreamIterator;
import static edu.ufl.cise.cop5555.sp12.Kind.*;



public class Scanner {

	private enum State {
		START, GOT_EQUALS, IDENT_PART, GOT_ZERO, GOT_LESS_THAN, GOT_GREATER_THAN, GOT_NOT_EQUAL, DIGITS, OPERATOR_PART, EOF, STRING_LITERAL,STRING_LITERAL_PART,COMMENT_START,COMMENT_END,
	};

	Map<String, Kind> reservedKeyWords=new HashMap<String, Kind>(16);


	public State state;
	char ch;
	public int index=0;
	public int l=0;
	public int begOffset=0;
	public int lineNum=1;

	TokenStream stream=null;

	Token t=null;

	char[] inputChars=null;
	List<Token> tokens=null;

	public Scanner(TokenStream stream)  {
		//TODO:  IMPLEMENT ME
		this.stream=stream;
		inputChars=stream.inputChars;
		tokens=stream.tokens;
	}



	public void scan() {
		//TODO:  IMPLEMENT ME

		reservedKeyWords.put("prog",Kind.PROG);
		reservedKeyWords.put("gorp",Kind.GORP);
		reservedKeyWords.put("string",Kind.STRING);
		reservedKeyWords.put("int",Kind.INT);
		reservedKeyWords.put("boolean",Kind.BOOLEAN);
		reservedKeyWords.put("map",Kind.MAP);
		reservedKeyWords.put("if",Kind.IF);
		reservedKeyWords.put("else",Kind.ELSE);
		reservedKeyWords.put("fi",Kind.FI);
		reservedKeyWords.put("do",Kind.DO);
		reservedKeyWords.put("od",Kind.OD);
		reservedKeyWords.put("print",Kind.PRINT);
		reservedKeyWords.put("println",Kind.PRINTLN);
		reservedKeyWords.put("true",Kind.BOOLEAN_LITERAL);
		reservedKeyWords.put("false",Kind.BOOLEAN_LITERAL);
		
		do {
			t = next();
			stream.tokens.add(t);
			t.lineNumber=lineNum;
//			System.out.print(t);
//			System.out.println(t.lineNumber);

		} while (!t.kind.equals(EOF));
	}

	public Token next(){

		Token t=null;

		state=State.START;

		do{


			switch(state){

			case START:
				begOffset = index;
				ch=getChar();
				switch (ch){

				case 0x1a:
					t=stream.new Token(Kind.EOF,begOffset,index);
					break;

				case ' ': case '\t': case '\f': 
					break;//white space

					//Line numbers
				case '\n':
					lineNum++;
					break;

				case '\r':
					ch=getChar();
					if(ch=='\n'){
						lineNum++;
						//index--; //have to treat \r\n as a single character
					}
					else {
						lineNum++;
						goToPreviousChar(); //have to go back to previous character
					}
					break;

				case '.':
					t=stream.new Token(Kind.DOT,begOffset,index);
					break;//token created

				case ';':
					t=stream.new Token(Kind.SEMI,begOffset,index);
					break;//token created

				case ',':
					t=stream.new Token(Kind.COMMA,begOffset,index);
					break;//token created

				case '(':
					t=stream.new Token(Kind.LEFT_PAREN,begOffset,index);
					break;//token created

				case ')':
					t=stream.new Token(Kind.RIGHT_PAREN,begOffset,index);
					break;//token created

				case '[':
					t=stream.new Token(Kind.LEFT_SQUARE,begOffset,index);
					break;//token created

				case ']':
					t=stream.new Token(Kind.RIGHT_SQUARE,begOffset,index);
					break;//token created

				case ':':
					t=stream.new Token(Kind.COLON,begOffset,index);
					break;//token created

				case '{':
					t=stream.new Token(Kind.LEFT_BRACE,begOffset,index);
					break;//token created

				case '}':
					t=stream.new Token(Kind.RIGHT_BRACE,begOffset,index);
					break;//token created

				case '|':
					t=stream.new Token(Kind.OR,begOffset,index);
					break;//token created

				case '&':
					t=stream.new Token(Kind.AND,begOffset,index);
					break;//token created

				case '+':
					t=stream.new Token(Kind.PLUS,begOffset,index);
					break;//token created

				case '-':
					t=stream.new Token(Kind.MINUS,begOffset,index);
					break;//token created

				case '*':
					t=stream.new Token(Kind.TIMES,begOffset,index);
					break;//token created

				case '/':
					t=stream.new Token(Kind.DIVIDE,begOffset,index);
					break;//token created

					//Operator parts
				case '=': 
					state=State.GOT_EQUALS;
					break;
				case '!':
					state=State.GOT_NOT_EQUAL;
					break;
				case '<': 
					state=State.GOT_LESS_THAN;
					break;
				case '>':		
					state=State.GOT_GREATER_THAN;
					break;

					//String literals
				case '"':
					state=State.STRING_LITERAL;
					break;

					//Comment part	
				case '#':
					state=State.COMMENT_START;
					break;

				case '0':
					t=stream.new Token(Kind.INTEGER_LITERAL,begOffset,index);
					break;
					//Escape sequence
				case '\\':
					ch=getChar();
					if(ch=='t'|ch=='n'|ch=='f'|ch=='r'){

					}
					else{
						if(ch!=0x1a)
							goToPreviousChar();
						t=stream.new Token(Kind.ILLEGAL_CHAR,begOffset,index);
					}break;

				default:
					if (Character.isDigit(ch)) {
						state = State.DIGITS;
					} else if (Character.isJavaIdentifierStart(ch)) {
						state = State.IDENT_PART;
					} else {
						t=stream.new Token(Kind.ILLEGAL_CHAR,begOffset,index);
					}

				}break; //End of START

			case DIGITS:
				ch=getChar();
				switch(ch){
				default:
					if(Character.isDigit(ch))
						break;
					else {
						if(ch!=0x1a)
							goToPreviousChar();
						t=stream.new Token(Kind.INTEGER_LITERAL,begOffset,index);
						state=State.START;
					}
				}break;//End of Digits

			case IDENT_PART:
				ch=getChar();

				if(Character.isJavaIdentifierStart(ch) | Character.isDigit(ch))
					break;//loop continues

				else {

					if(ch!=0x1a) //as it is EOF, no need of going to back
						goToPreviousChar();

					String sub=getSubString(begOffset,index);
					if(reservedKeyWords.containsKey(sub)){
						t=stream.new Token(reservedKeyWords.get(sub),begOffset,index);
					}
					else
						t=stream.new Token(Kind.IDENTIFIER,begOffset,index);
				}
				break;//End of ident part

			case GOT_EQUALS:
				ch=getChar();
				if(ch=='=')
					t=stream.new Token(Kind.EQUALS,begOffset,index);
				else{
					if(ch!=0x1a)
						goToPreviousChar();//its assignment or not or lessthan or greaterthan operators, so goto previous character
					t=stream.new Token(Kind.ASSIGN,begOffset,index);
				}
				break;

			case GOT_LESS_THAN:
				ch=getChar();
				if(ch=='=')
					t=stream.new Token(Kind.AT_MOST,begOffset,index);
				else{
					if(ch!=0x1a)
						goToPreviousChar();//its assignment or not or lessthan or greaterthan operators, so goto previous character
					t=stream.new Token(Kind.LESS_THAN,begOffset,index);
				}
				break;

			case GOT_GREATER_THAN:
				ch=getChar();
				if(ch=='=')
					t=stream.new Token(Kind.AT_LEAST,begOffset,index);
				else{
					if(ch!=0x1a)
						goToPreviousChar();//its assignment or not or lessthan or greaterthan operators, so goto previous character
					t=stream.new Token(Kind.GREATER_THAN,begOffset,index);
				}
				break;

			case GOT_NOT_EQUAL:
				ch=getChar();
				if(ch=='=')
					t=stream.new Token(Kind.NOT_EQUALS,begOffset,index);
				else{
					if(ch!=0x1a)
						goToPreviousChar();//its assignment or not or lessthan or greaterthan operators, so goto previous character
					t=stream.new Token(Kind.NOT,begOffset,index);
				}
				break;

			case COMMENT_START:
				ch=getChar();

				if(ch=='#')
					state=State.COMMENT_END;
				else {
					t=stream.new Token(Kind.MALFORMED_COMMENT,begOffset,index);
					state=State.START;
					if(ch!=0X1a)
					goToPreviousChar();
				}
				break;

			case COMMENT_END:
				ch=getChar();
				
					if(ch=='#'){
						ch=getChar();
						if(ch=='#'){
							//t=stream.new Token(Kind.COMMENT,begOffset,index); //for Comment, no need of adding to the list
							state=State.START;
						}
						else if(ch==0x1a)
							t=stream.new Token(Kind.MALFORMED_COMMENT,begOffset,index);
					}
					else if(ch==0x1a)
						t=stream.new Token(Kind.MALFORMED_COMMENT,begOffset,index);

				break;

			case STRING_LITERAL:
				ch=getChar();
				switch(ch){
				case '\\':
					state=State.STRING_LITERAL_PART;
					break;
				case '"':
					t=stream.new Token(Kind.STRING_LITERAL,begOffset,index);
					break;
				case '\n': case '\r':
					t=stream.new Token(Kind.MALFORMED_STRING,begOffset,index);
					break;
				case 0x1a:
					t=stream.new Token(Kind.MALFORMED_STRING,begOffset,index);
				default:
					break;

				}
				break; //End of String literal

			case STRING_LITERAL_PART:
				ch=getChar();
				switch(ch){
				case 't': case 'n': case 'f': case 'r': case '"': case '\\':
					state=State.STRING_LITERAL;
					break;
				default:
					if(ch!=0x1a)
						goToPreviousChar();
					t=stream.new Token(Kind.MALFORMED_STRING,begOffset,index);
					break;
				}
				break;//End of String literal part
			}

		}while(t==null);
		
		return t;
	}

	public char getChar(){

		if(index<inputChars.length)
			return(inputChars[index++]);
		else
			return(0x1a);
	}

	public void goToPreviousChar(){
		if(index>0)
			index--;
	}

	public String getSubString(int i, int j){

		return new String(inputChars).substring(i, j);

	}
	//You will need to add fields and additional methods

}
