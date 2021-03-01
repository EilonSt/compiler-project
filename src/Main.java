

import ast.*;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        try {
            var inputMethod = args[0];
            var action = args[1];
            var filename = args[args.length - 2];
            var outfilename = args[args.length - 1];

            Program prog;

            if (inputMethod.equals("parse")) {
                throw new UnsupportedOperationException("TODO - Ex. 4");
            } else if (inputMethod.equals("unmarshal")) {
                AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                prog = xmlSerializer.deserialize(new File(filename));
            } else {
                throw new UnsupportedOperationException("unknown input method " + inputMethod);
            }

            var outFile = new PrintWriter(outfilename);
            try {

                if (action.equals("marshal")) {
                    AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                    xmlSerializer.serialize(prog, outfilename);
                } else if (action.equals("print")) {
                    AstPrintVisitor astPrinter = new AstPrintVisitor();
                    astPrinter.visit(prog);
                    outFile.write(astPrinter.getString());

                } else if (action.equals("semantic")) {
                    AstSemanticChecksVisitor astSemantic = new AstSemanticChecksVisitor(outFile);
                    astSemantic.visit(prog);
                    outFile.write("OK\n");

                } else if (action.equals("compile")) {
                    //AstPrintVisitor astPrinter = new AstPrintVisitor();
                    AstSymbolTableVisitor astSymbolTable = new AstSymbolTableVisitor();
                    astSymbolTable.visit(prog);
                    AstVTVisitor VTvisitor = new AstVTVisitor();
                    AstLLVMVisitor LLVMvisitor = new AstLLVMVisitor();
                    VTvisitor.visit(prog);
                    LLVMvisitor.visit(prog);
                    outFile.write(LLVMvisitor.getString());

                } else if (action.equals("rename")) {
                    var type = args[2];
                    var originalName = args[3];
                    var Line = args[4];
                    var newName = args[5];

                    boolean isMethod;
                    if (type.equals("var")) {
                        isMethod = false;
                    } else if (type.equals("method")) {
                        isMethod = true;
                    } else {
                        throw new IllegalArgumentException("unknown rename type " + type);
                    }

                    //OUR CODE
                    AstSymbolTableVisitor astSymbolTable = new AstSymbolTableVisitor();
                    astSymbolTable.visit(prog);    
                    if (isMethod){
                        AstMethodRenameVisitor astMethodRename = new AstMethodRenameVisitor(originalName,Integer.parseInt(Line),newName);
                        astMethodRename.visit(prog);
                    }
                    else{
                        AstVarRenameVisitor astVarRename=new AstVarRenameVisitor(originalName,Integer.parseInt(Line),newName);
                        astVarRename.visit(prog);
                    }
					
					/*to print modified file as an xml (which is what we should do), use these two lines:*/
					//AstXMLSerializer xmlSerializer = new AstXMLSerializer();
					//xmlSerializer.serialize(prog, outfilename);
					
					/*to print as a java program (for easier debugging), use these 3 lines instead of the 2 lines above this comment	*/				
                    AstPrintVisitor astPrinter = new AstPrintVisitor();
                    astPrinter.visit(prog);
                    outFile.write(astPrinter.getString());
                    

                    //END OF OUR CODE

                    
                } else {
                    throw new IllegalArgumentException("unknown command line action " + action);
                }
            } finally {
                outFile.flush();
                outFile.close();
            }

        } catch (FileNotFoundException e) {
            System.out.println("Error reading file: " + e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("General error: " + e);
            e.printStackTrace();
        }
    }
}
