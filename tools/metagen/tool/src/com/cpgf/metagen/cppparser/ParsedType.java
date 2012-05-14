package com.cpgf.metagen.cppparser;

import java.util.ArrayList;
import java.util.List;

public class ParsedType {
	private String literalType;
	private List<TypeToken> tokenList;

	private String baseType;
	private List<String> arrayDimensions;
	private List<EnumCompoundType> pointers;
	private EnumCompoundType reference;
	private EnumCompoundType rValueReference;

	public ParsedType(String literalType) {
		this.literalType = literalType;

		this.doParse();
	}

	public String getLiteralType() {
		return literalType;
	}

	public void setLiteralType(String literalType) {
		this.literalType = literalType;
	}
	
	public String getBaseType() {
		return baseType;
	}
	
	public String getNormalizedType() {
		String result = "";
		
		TypeToken previousToken = null;

		for(int i = 0; i < this.tokenList.size(); ++i) {
			TypeToken token = this.tokenList.get(i);
			if(previousToken != null) {
				if(previousToken.isLiteralText() && token.isLiteralText()
						|| previousToken.getKind() == EnumTypeTokenKind.RightAngle && token.getKind() == EnumTypeTokenKind.RightAngle) {
					result = result + " ";
				}
			}
			result = result + token.getToken();
			previousToken = token;
		}
		
		return result;
	}
	
	public int getArrayDimension() {
		if(this.arrayDimensions == null) {
			return 0;
		}
		else {
			return this.arrayDimensions.size();
		}
	}
	
	public boolean isArray() {
		return this.getArrayDimension() > 0;
	}
	
	public String getDimensionAt(int index) {
		return this.arrayDimensions.get(index);
	}

	public EnumCompoundType getReference() {
		return reference;
	}

	public EnumCompoundType getRValueReference() {
		return rValueReference;
	}

	public int getPointerCount() {
		if(this.pointers == null) {
			return 0;
		}
		else {
			return this.pointers.size();
		}
	}

	public EnumCompoundType getPointerAt(int index) {
		return this.pointers.get(index);
	}

	public boolean isPointer() {
		return this.getPointerCount() > 0;
	}

	public boolean isReference() {
		return this.getReference() != EnumCompoundType.None
			|| this.getRValueReference() != EnumCompoundType.None;
	}

	public boolean isPointerOrReference() {
		return this.isPointer()
			|| this.getReference() != EnumCompoundType.None
			|| this.getRValueReference() != EnumCompoundType.None;
	}

	private void doParse() {
		this.tokenList = ParserUtil.splitTypeTokens(ParserUtil.splitTypeTokenLiterals(this.literalType));

		ParserUtil.normalizeConstVolatile(this.tokenList);
		ParserUtil.normalizeSignedAndUnsigned(this.tokenList);
		ParserUtil.normalizeShortInt(this.tokenList);
		ParserUtil.normalizeUnusedKeyword(this.tokenList);
		
		this.baseType = findBaseType(this.tokenList);
		this.arrayDimensions = parseArrayDimensions(this.tokenList);
		this.parsePointerAndReference();
	}

	private void parsePointerAndReference() {
		this.reference = EnumCompoundType.None;
		this.rValueReference = EnumCompoundType.None;

		int nestedDepth = 0;
		
		EnumCompoundType cv = EnumCompoundType.NoCV;
		
		for(int i = 0; i < this.tokenList.size(); ++i) {
			TypeToken token = tokenList.get(i);

			if(token.isLeftBracket()) {
				++nestedDepth;
			}
			else if(token.isRightBracket()) {
				--nestedDepth;
			}

			if(nestedDepth == 0) {
				if(token.getKind() == EnumTypeTokenKind.Const) {
					cv = EnumCompoundType.add(cv, EnumCompoundType.Const);
				}
				else if(token.getKind() == EnumTypeTokenKind.Volatile) {
					cv = EnumCompoundType.add(cv, EnumCompoundType.Volatile);
				}
				else if(token.isPointerOrReference()) {
					if(token.getKind() == EnumTypeTokenKind.Pointer) {
						if(this.pointers == null) {
							this.pointers = new ArrayList<EnumCompoundType>();
						}
						this.pointers.add(cv);
					}
					else if(token.getKind() == EnumTypeTokenKind.Reference) {
						this.reference = cv;
					}
					else if(token.getKind() == EnumTypeTokenKind.RValueReference) {
						this.rValueReference = cv;
					}

					cv = EnumCompoundType.NoCV;
				}
			}
		}
	}

	private static String findBaseType(List<TypeToken> tokenList) {
		for(TypeToken token : tokenList) {
			if(token.getKind() == EnumTypeTokenKind.Symbol) {
				return token.getToken();
			}
		}
		
		return "";
	}

	private static List<String> parseArrayDimensions(List<TypeToken> tokenList) {
		List<String> dimensions = null;
		int count = tokenList.size();
		int nestedDepth = 0;
		
		for(int i = 0; i < count - 1; ++i) {
			TypeToken token = tokenList.get(i);
			
			if(token.getKind() == EnumTypeTokenKind.LeftAngle || token.getKind() == EnumTypeTokenKind.LeftParenthesis) {
				++nestedDepth;
			}
			
			if(token.getKind() == EnumTypeTokenKind.RightAngle || token.getKind() == EnumTypeTokenKind.RightParenthesis) {
				--nestedDepth;
			}
			
			if(nestedDepth == 0 && token.getKind() == EnumTypeTokenKind.LeftSquare) {
				if(dimensions == null) {
					dimensions = new ArrayList<String>();
				}
				
				String dim = "";
				int k = i + 1;
				for(; k < count; ++k) {
					TypeToken nextToken = tokenList.get(k);
					if(nextToken.getKind() == EnumTypeTokenKind.RightSquare) {
						break;
					}
					
					if(dim.length() > 0) {
						dim = dim + " ";
					}
					
					dim = dim + nextToken.getToken();
				}
				
				dimensions.add(dim);

				i = k;
			}
		}

		return dimensions;
	}

}