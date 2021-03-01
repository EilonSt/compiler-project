package ast;

import java.util.ArrayList;


public class VTMethodsEntry {
    private String method_name;
    private String class_name;
    private String ret_type;
    private ArrayList<String> params;

    /*Translates object java type to LLVM type*/
    private String getLLVMType(String type){
        String ret = null;
        switch (type) {
            case "int-literal":
                ret = "i32";
                break;
            case "int":
                ret = "i32";
                break;
            case "int-array":
                ret = "i32*";
                break;
            case "bool":
                ret = "i1";
                break;
            case "ref":
                ret = "i8*";
                break;
            default:
                ret = "i8*";
        }
        return ret;
    }

    /*Builder*/
    public VTMethodsEntry(String mn,String cn,String rt){
        method_name = mn;
        class_name = cn;
        ret_type = rt;
        params = new ArrayList<String>();
    }

    public ArrayList<String> getParams(){
        return this.params;
    }

    public void addParam(String type){
        params.add(type);
    }

    public void setClassName(String class_name){
        this.class_name = class_name;
    }

    public String getClassName(){
        return this.class_name;
    }
    public String getMethodName(){
        return this.method_name;
    }
    public String getRetType(){
        return this.ret_type;
    }

    /*Returns a string which represents the method entry's pointer type (according to it's return type and param types)*/
    public String getMethodPtrType(){
        String methodPtrType = "";
        methodPtrType += getLLVMType(ret_type);
        methodPtrType += " (i8*"; //method's first parameter is always "this"
        int numOfParams = params.size();
        if (numOfParams > 0){
            methodPtrType += ", ";
        }
        for(int i=0;i<numOfParams-1;i++){
            String param = params.get(i);
            methodPtrType += getLLVMType(param);
            methodPtrType += ", ";
        }
        if (numOfParams > 0){
            methodPtrType += getLLVMType(params.get(numOfParams-1));
        }
        methodPtrType += ")*";
        return methodPtrType;
    }

}


