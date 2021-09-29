import {
  continuedIndent,
  flatIndent,
  foldInside,
  foldNodeProp,
  indentNodeProp,
  LanguageSupport,
  LRLanguage
} from "@codemirror/language"
import {styleTags, Tag, tags as t} from "@codemirror/highlight"

// Make sure that the code is generated (npm run build-grammar)
import {parser} from "./parser";

export const customTags = {
  call: Tag.define(),
  annotationAttribute: Tag.define()
}

export const javaLanguage = LRLanguage.define({
  parser: parser.configure({
    props: [
      indentNodeProp.add({
        IfStatement: continuedIndent({except: /^\s*({|else\b)/}),
        TryStatement: continuedIndent({except: /^\s*({|catch|finally)\b/}),
        LabeledStatement: flatIndent,
        SwitchBlock: context => {
          let after = context.textAfter, closed = /^\s*\}/.test(after),
            isCase = /^\s*(case|default)\b/.test(after)
          return context.baseIndent + (closed ? 0 : isCase ? 1 : 2) * context.unit
        },
        BlockComment: () => -1,
        Statement: continuedIndent({except: /^{/})
      }),
      foldNodeProp.add({
        ["Block SwitchBlock ClassBody ElementValueArrayInitializer ModuleBody EnumBody " +
        "ConstructorBody InterfaceBody ArrayInitializer"]: foldInside,
        BlockComment(tree) {
          return {from: tree.from + 2, to: tree.to - 2}
        }
      }),
      styleTags({
        instanceof: t.operatorKeyword,
        "new var assert": t.keyword,
        "class record interface extends implements module package import enum": t.definitionKeyword,
        "switch const goto yield while for if else case default do break continue return try catch finally throw": t.controlKeyword,
        ["requires exports opens uses provides public private protected static transitive abstract final " +
        "strictfp synchronized native transient volatile throws"]: t.modifier,
        "this super null BooleanLiteral": t.constant(t.variableName),
        IntegerLiteral: t.integer,
        FloatLiteral: t.float,
        'StringLiteral StringBlockLiteral': t.string,
        CharacterLiteral: t.character,
        LineComment: t.lineComment,
        BlockComment: t.blockComment,
        BooleanLiteral: t.bool,
        'ElementValuePair/Identifier': customTags.annotationAttribute,
        'void PrimitiveType': t.standard(t.typeName),
        'TypeName': t.typeName,
        Identifier: t.variableName,
        'Annotation Annotation/Identifier MarkerAnnotation MarkerAnnotation/Identifier': t.annotation,
        "FormalParameter/Definition": t.function(t.definition(t.variableName)),
        "MethodName/Identifier": t.function(t.variableName),
        'MethodInvocation/MethodName/Identifier': t.function(customTags.call),
        Definition: t.definition(t.variableName),
        ArithOp: t.arithmeticOperator,
        LogicOp: t.logicOperator,
        BitOp: t.bitwiseOperator,
        CompareOp: t.compareOperator,
        AssignOp: t.definitionOperator,
        UpdateOp: t.updateOperator,
        Asterisk: t.punctuation,
        Label: t.labelName,
        "( )": t.paren,
        "[ ]": t.squareBracket,
        "{ }": t.brace,
        ".": t.derefOperator,
        ", ;": t.separator
      })
    ]
  }),
  languageData: {
    commentTokens: {line: "//", block: {open: "/*", close: "*/"}},
    indentOnInput: /^\s*(?:case |default:|\{|\})$/
  }
})

export function java() {
  return new LanguageSupport(javaLanguage)
}
