/***************************/
/* Based on a template by Oren Ish-Shalom */
/***************************/

/*************/
/* USER CODE */
/*************/
import java_cup.runtime.*;



/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%%

/************************************/
/* OPTIONS AND DECLARATIONS SECTION */
/************************************/

/*****************************************************/
/* Lexer is the name of the class JFlex will create. */
/* The code will be written to the file Lexer.java.  */
/*****************************************************/
%class Lexer

/********************************************************************/
/* The current line number can be accessed with the variable yyline */
/* and the current column number with the variable yycolumn.        */
/********************************************************************/
%line
%column

/******************************************************************/
/* CUP compatibility mode interfaces with a CUP generated parser. */
/******************************************************************/
%cup

/****************/
/* DECLARATIONS */
/****************/
/*****************************************************************************/
/* Code between %{ and %}, both of which must be at the beginning of a line, */
/* will be copied verbatim (letter to letter) into the Lexer class code.     */
/* Here you declare member variables and functions that are used inside the  */
/* scanner actions.                                                          */
/*****************************************************************************/
%{
	/*********************************************************************************/
	/* Create a new java_cup.runtime.Symbol with information about the current token */
	/*********************************************************************************/
	private Symbol symbol(int type)               {return new Symbol(type, yyline, yycolumn);}
	private Symbol symbol(int type, Object value) {return new Symbol(type, yyline, yycolumn, value);}

	/*******************************************/
	/* Enable line number extraction from main */
	/*******************************************/
	public int getLine()    { return yyline + 1; }
	public int getCharPos() { return yycolumn;   }
%}

/***********************/
/* MACRO DECALARATIONS */
/***********************/
InputCharacter 				= [^\r\n]
LineTerminator 				= \r|\n|\r\n
WhiteSpace 					= {LineTerminator} | [ \t\f]
ID 					= [a-zA-Z][a-zA-Z0-9_]*
INTEGER				= 0 | [1-9][0-9]*
SingleLineComment 			= "//" {InputCharacter}* {LineTerminator}?
LongComment 			= "/*" [^*] ~"*/" | "/*" "*"+ "/"

/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%%

/************************************************************/
/* LEXER matches regular expressions to actions (Java code) */
/************************************************************/

/**************************************************************/
/* YYINITIAL is the state at which the lexer begins scanning. */
/* So these regular expressions will only be matched if the   */
/* scanner is in the start state YYINITIAL.                   */
/**************************************************************/

<YYINITIAL> {
"public"            	{ return symbol(sym.PUBLIC); }
"static" 				{ return symbol(sym.STATIC); }
"void"					{ return symbol(sym.VOID); }
"main"					{ return symbol(sym.MAIN); }
"String"				{ return symbol(sym.STRING); }
"class"					{ return symbol(sym.CLASS); }
"extends"				{ return symbol(sym.EXTENDS); }
"new"					{ return symbol(sym.NEW); }
"length"				{ return symbol(sym.LENGTH); }
"return"				{ return symbol(sym.RETURN); }
"System.out.println" 	{ return symbol(sym.PRINTLN); }
"if"					{ return symbol(sym.IF); }
"else"             		{ return symbol(sym.ELSE); }
"while"					{ return symbol(sym.WHILE); }
"int"					{ return symbol(sym.INT); }
"boolean"				{ return symbol(sym.BOOLEAN); }
"this"					{ return symbol(sym.THIS); }
"true"					{ return symbol(sym.TRUE); }
"false"					{ return symbol(sym.FALSE); }
"+"						{ return  symbol(sym.PLUS); }
"-"						{ return symbol(sym.MINUS); }
"*"						{ return symbol(sym.MULT); }
"!"						{ return symbol(sym.NOT); }
"<"						{ return symbol(sym.LT); }
"&&"					{ return symbol(sym.AND); }
"="						{ return  symbol(sym.ASSIGN); }
"."						{ return symbol(sym.DOT); }
","						{ return symbol(sym.COMMA); }
";"						{ return symbol(sym.SEMICOLON); }
"("						{ return symbol(sym.LPAREN); }
")"						{ return symbol(sym.RPAREN); }
"["						{ return symbol(sym.LSQUARE); }
"]"						{ return symbol(sym.RSQUARE); }
"{"						{ return symbol(sym.LCURLY); }
"}"						{ return symbol(sym.RCURLY); }
{ID}				    { return symbol(sym.ID, new String(yytext())); }
{INTEGER}			    { return symbol(sym.NUMBER, Integer.parseInt(yytext())); }
{WhiteSpace}            { /*  do nothing  */ }
<<EOF>>				    { return symbol(sym.EOF); }
{LongComment}           { /*  do nothing */ }
{SingleLineComment}		{ /*  do nothing  */ }
}

[^]                     { throw new Error("Illegal character <"+ yytext() + ">"); }