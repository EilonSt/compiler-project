package ast;

import java.util.HashSet;
import java.util.Set;

//import sun.jvm.hotspot.debugger.cdbg.Sym;

public class SymbolTable {
    private String className;
    private Set<SymbolTableEntry> entries;
    private SymbolTable parentSymbolTable;

    public SymbolTable(){
        parentSymbolTable = null;
        entries = new HashSet<SymbolTableEntry>();
    }

    public void setSTclassName(String className){
        this.className = className;
    }

    public String getSTclassName(){
        return this.className;
    }


    public void setParent(SymbolTable p){
        this.parentSymbolTable = p;
    }

    public void addEntry(String symbol, String kind,String Decl, int lineNum){
        SymbolTableEntry sy = new SymbolTableEntry(symbol,kind,Decl,lineNum);
        entries.add(sy);
    }

    public SymbolTable getParent(){
        return parentSymbolTable;
    }

    public SymbolTableEntry getEntry(String symbol, int lineNum){
        for(SymbolTableEntry entr : entries){
            if(entr.getSymbol().equals(symbol) && entr.getLineNum() == lineNum){
                return entr;
            }
        }
        /*ERROR, never should reach here */
        return null;
    }

    /*used in AstVarRenameVisitor*/
    public boolean checkEntryExists(String symbol){
        for(SymbolTableEntry entr : entries){
            if(entr.getSymbol().equals(symbol)){
                return true;
            }
        }
        return false;
    }

    /*used in AstMethodRenameVisitor
    checks if the entry we find with the matchind symbol is a method*/
    public boolean checkMethodDeclEntryExists(String symbol){
        for(SymbolTableEntry entr : entries){
            if(entr.getSymbol().equals(symbol) && entr.getKind().equals("method")){
                return true;
            }
        }
        return false;
    }

    /*climb up ST tree to find method decleration. returns the ST entry of the method decleration*/
    public SymbolTableEntry getMethodsSTEntry(String methodName){
        SymbolTable curr = this;
        curr.toPrint();
        while(curr != null){/*search for method in curr*/
            curr.toPrint();
            for(SymbolTableEntry entr : curr.entries){
                if(entr.getSymbol().equals(methodName) && entr.getKind().equals("method")){
                    return entr;
                }
            }
            /*method not found, proceed to parent's ST*/
            curr = curr.parentSymbolTable;
        }
        return null;
    }

    /*returns true if identifier is field, false ow */
    public boolean isField(String identifier){
        boolean exists = true;
        for(SymbolTableEntry entr : this.entries){
            if(entr.getSymbol().equals(identifier)){
                exists = false;
                break;
            }
        }
        return exists;
    }

    /* climb up ST tree to find the STentry in which the identifier was declared.
    returns the identifier's class (Decl column)*/
    public String getIdentifiersClass(String identifier){
        String identifierClass;
        SymbolTable curr = this;
        while(curr != null){/*search for identifier decl in curr*/
            for(SymbolTableEntry entr : curr.entries){
                if(entr.getSymbol().equals(identifier)){
                    String kind = entr.getKind();
                    if(kind.equals("field") || kind.equals("var") || kind.equals("formal")){/*not sure if this is correct*/
                        identifierClass = entr.getDecl();
                        return identifierClass;
                    }
                }
            }
            /*variable not found, proceed to parent's ST*/
            curr = curr.parentSymbolTable;
        }
        return null;
    }



    /*prints ST entries of the ST on which it was called*/
    public String toPrint(){
        int cnt = 1;
        String fmessage = "";
        for (SymbolTableEntry entry : entries){
            String message = Integer.toString(cnt);
            message = message + " " + entry.get_symbol_kind_Decl();
            fmessage = fmessage + message + "\n";
            cnt ++;
        }
        return fmessage;
    }

}

