package ast;

import java.util.ArrayList;


public class VTFields {
    private VTMethods VTM;
    private ArrayList<VTFieldsEntry> fields;
    private int curr_index = 8;


    public VTFields(){
        VTM = new VTMethods();
        fields = new ArrayList<VTFieldsEntry>();
    }

    public void addField(String name, String type, int byte_size){
        VTFieldsEntry vtfe = new VTFieldsEntry(name,type,byte_size,curr_index);
        fields.add(vtfe);
        curr_index += byte_size;
    }
    public VTFieldsEntry getFieldAtIndex(int i){
        return fields.get(i);
    }
    public ArrayList<VTFieldsEntry> getFields(){
        return fields;
    }
    public VTMethods getVT(){
        return this.VTM;
    }
    public int getCurrIndex(){
        return curr_index;
    }
    public void copyVTF(VTFields source){
        /*fields copy*/
        for (int i=0;i<source.fields.size();i++){
            VTFieldsEntry e = new VTFieldsEntry(source.getFieldAtIndex(i).getName(),source.getFieldAtIndex(i).getType(),
            source.getFieldAtIndex(i).getSize(),source.getFieldAtIndex(i).getStartIndex());
            this.fields.add(e);
        }
        /*index copy */
        this.curr_index = source.getCurrIndex();
        /*VTable copy */
        this.VTM.copyVT(source.VTM);
        
    }

    /*returns start index of field in vtable */
    public int getStartIndexByName(String fieldName){
        for (var field:fields){
            if(fieldName.equals(field.getName())){
                return field.getStartIndex();
            }
        }
        System.out.println("ERROR in getStartIndexByName VTFields");
        //shouldn't reach heree
        return -1;
    }
}
