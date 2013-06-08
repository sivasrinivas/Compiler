package edu.ufl.cise.cop5555.sp12.context;

import java.util.*;

import edu.ufl.cise.cop5555.sp12.ast.Declaration;

public class SymbolTable {

	public Stack<Integer> scopeStack=null;
	public LinkedList<Attribute> list=null;
	public Map<String,LinkedList<Attribute>> hashTable=null;
	int current_scope=0;
	int next_scope=0;

	public SymbolTable(){
		//TODO Implement me
		scopeStack=new Stack<Integer>();
		hashTable=new HashMap<String,LinkedList<Attribute>>();
	}


	public void enterScope() {
		current_scope=next_scope++;
		scopeStack.push(current_scope);
	}

	public void exitScope() {
		scopeStack.pop();
	}

	// returns the in-scope declaration of the name if there is one, 
	//otherwise it returns null
	public Declaration lookup(String ident) {
		if(hashTable.containsKey(ident)){
			LinkedList<Attribute> lList=hashTable.get(ident);
//			System.out.println("list collected"+lList.get(0).declaration.ident);
//			System.out.println("list collected"+lList.get(0).scopeNumber);
			
			for(int i=scopeStack.size()-1;i>=0;i--){
				for(Attribute att : lList){

					if(att.scopeNumber==scopeStack.get(i)){
						return att.declaration;
					}
				}
			}		
		}

		return null;
	}


	// if the name is already declared IN THE CURRENT SCOPE, returns false. 
	//Otherwise inserts the declaration in the symbol table
	public boolean insert(String ident, Declaration dec) {
		if(hashTable.containsKey(ident)){
			LinkedList<Attribute> lList;
			lList=hashTable.get(ident);
			for(Attribute att : lList){
				if(att.scopeNumber==scopeStack.peek())
					return false;
			}
			lList.addFirst(new Attribute(scopeStack.peek(), dec));
			hashTable.put(ident, lList);
//			System.out.println("proof for Inserted :"+hashTable.get(ident));
		}
		else{
			Attribute att=new Attribute(scopeStack.peek(),dec);
			LinkedList<Attribute> lList=new LinkedList<Attribute> ();
			lList.addFirst(att);
			hashTable.put(ident, lList);
//			System.out.println("proof for Inserted :"+hashTable.get(ident).get(0).declaration.ident.getText());
		}

		return true;
	}

	public class Attribute{
		int scopeNumber=0;
		Declaration declaration=null;

		public Attribute(int scopeNumber, Declaration declaration){
			this.scopeNumber=scopeNumber; 
			this.declaration=declaration;
		}
	}


}