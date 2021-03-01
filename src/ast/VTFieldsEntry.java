package ast;

public class VTFieldsEntry {

    private String name;
    private String type;
    private int byte_size;
    private int start_index;

/*BUILDER*/    
public VTFieldsEntry(String name, String type, int byte_size, int start_index){
    this.name = name;
    this.type = type;
    this.byte_size = byte_size;
    this.start_index = start_index;
}


public void setName(String name){
    this.name = name;
}

public String getName(){
    return this.name;
}

public void setType(String type){
    this.type = type;
}

public String getType(){
    return this.type;
}
public int getSize(){
    return this.byte_size;
}
public int getStartIndex(){
    return this.start_index;
}


}