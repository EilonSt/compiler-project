package ast;
import java.util.ArrayList;

public class VTMethods {

    private ArrayList<VTMethodsEntry> methodecls;

    public VTMethods(){
        this.methodecls = new ArrayList<VTMethodsEntry>();
    }

    public int addMethodDecl(String method_name, String class_name,String ret_type){
        VTMethodsEntry e = new VTMethodsEntry(method_name,class_name,ret_type);
        methodecls.add(e);
        return methodecls.size() - 1;
    }

    public ArrayList<VTMethodsEntry> getMethods(){
        return this.methodecls;
    }

    public int getNumOfMethods(){
        return this.methodecls.size();
    }

    public void copyVT(VTMethods source){
        for(int i=0; i<source.getMethods().size(); i++){
            VTMethodsEntry methodEntry = source.getMethods().get(i);
            addMethodDecl(methodEntry.getMethodName(),methodEntry.getClassName(),methodEntry.getRetType());
            ArrayList<String> methodParams = methodEntry.getParams();
            int numParams = methodParams.size();

            //copy params
            VTMethodsEntry ourMethodEntry = methodecls.get(i);
            for (int j=0;j<numParams;j++){
                String param = methodParams.get(j);
                ourMethodEntry.addParam(param);
            }
        }
        
    }

    public VTMethodsEntry EntryAtIndex(int i){
        return methodecls.get(i);
    }

    /*Checks it method named "s" exists in VTM. if it does returns it's index, o\w returns -1*/
    public int methodExist(String s){
        int not_exist = -1;
        for(int i=0;i<methodecls.size();i++){
            if(methodecls.get(i).getMethodName().equals(s)){
                return i;
            }
        }
        return not_exist;
    }

    /*Returns a string which represents the method entry (at given index) pointer type (according to it's return type and param types)*/
    public String getMethodPtrType(int methodEntryIndex){
        VTMethodsEntry methodEntry = methodecls.get(methodEntryIndex);
        return methodEntry.getMethodPtrType();
    }


}
