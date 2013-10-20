package com.sdc.ast;

public enum ExpressionType {
    ADD, SUB, MUL, DIV, REM,
    INC, DEC, INC_REV, DEC_REV, ADD_INC, SUB_INC, MUL_INC, DIV_INC, REM_INC,
    SHR_INC, SHL_INC, USHR_INC, BITWISE_OR_INC, BITWISE_XOR_INC, BITWISE_AND_INC,
    AND, OR, NOT, NEGATE,
    EQ, NE, GE, GT, LE, LT,
    BITWISE_AND, BITWISE_OR, BITWISE_XOR,
    SHR, SHL, USHR,
    DOUBLE_CAST, INT_CAST, LONG_CAST, SHORT_CAST, BYTE_CAST, CHAR_CAST, FLOAT_CAST, CHECK_CAST,
    ARRAYLENGTH, NEWARRAY, SQUARE_BRACKETS,
    VARIABLE, FIELD, CONST,
    TERNARY_IF, INVOCATION, NEW, INSTANCEOF
}
