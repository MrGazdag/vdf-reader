package me.mrgazdag.programs.vdfreader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class VDFObject {
    private final Map<String,Object> map;

    public VDFObject() {
        this.map = new LinkedHashMap<>();
    }

    public VDFObject(String data) {
        this(new StringReader(data));
    }

    public VDFObject(StringReader sr) {
        this();

        while (sr.canRead()) {
            sr.skipWhitespace();
            if (sr.peek() == '}') break; //break recursiveness

            if (!sr.canRead()) break;
            if (sr.peek() != '"') throw EXPECTED_FOUND("key start quote", sr);
            sr.skip(); //"
            StringBuilder key = new StringBuilder();
            boolean escaping = false;
            while (sr.canRead()) {
                char c = sr.read();
                if (escaping) {
                    if (c == '"') key.append('"');
                    else if (c == 't') key.append('\t');
                    else if (c == 'n') key.append('\n');
                    else if (c == '\\') key.append('\\');
                    else throw EXPECTED_FOUND("valid escape sequence", sr);

                    escaping = false;
                } else {
                    if (c == '\\') escaping = true;
                    else if (c == '"') break;
                    else key.append(c);
                }
            }
            sr.skipWhitespace();
            if (!sr.canRead()) throw EXPECTED("value", sr);
            if (sr.peek() == '{') {
                //object, read recursively
                sr.skip(); //{
                if (!sr.canRead()) throw EXPECTED("object body", sr);
                VDFObject obj = new VDFObject(sr);
                map.put(key.toString(), obj);
            } else if (sr.peek() == '"') {
                sr.skip(); //"
                //string value
                StringBuilder value = new StringBuilder();
                escaping = false;
                while (sr.canRead()) {
                    char c = sr.read();
                    if (escaping) {
                        if (c == '"') value.append('"');
                        else if (c == 't') value.append('\t');
                        else if (c == 'n') value.append('\n');
                        else if (c == '\\') value.append('\\');
                        else throw EXPECTED_FOUND("valid escape sequence", sr);

                        escaping = false;
                    } else {
                        if (c == '\\') escaping = true;
                        else if (c == '"') break;
                        else value.append(c);
                    }
                }
                map.put(key.toString(), value.toString());
                sr.skip();//}
            }
        }
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public VDFObject putString(String key, String value) {
        map.put(key, value);
        return this;
    }

    public VDFObject putObj(String key, VDFObject value) {
        map.put(key, value);
        return this;
    }

    public String getString(String key) {
        return (String) map.get(key);
    }

    public VDFObject getObject(String key) {
        return (VDFObject) map.get(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toStringInternal(sb, 0);
        return sb.toString();
    }

    private void toStringInternal(StringBuilder sb, int tabsAmount) {
        String tabString = "\t".repeat(tabsAmount);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(tabString);

            sb.append('"').append(entry.getKey()).append('"');
            if (entry.getValue() instanceof String s) {
                sb.append("\t\t\"").append(s).append("\"\n");
            } else if (entry.getValue() instanceof VDFObject obj) {
                sb.append('\n');
                sb.append(tabString).append("{\n");
                obj.toStringInternal(sb,tabsAmount+1);
                sb.append(tabString).append("}\n");
            }
        }
    }

    private static RuntimeException EXPECTED(String expected, StringReader sr) {
        return new IllegalArgumentException("Expected " + expected + " at " + sr.getPos());
    }
    private static RuntimeException EXPECTED_FOUND(String expected, StringReader sr) {
        return new IllegalArgumentException("Expected " + expected + ", found " + sr.peek() + " at " + sr.getPos());
    }

    private static class StringReader {
        private final char[] chars;
        private int pos;

        public StringReader(String str) {
            this.chars = str.toCharArray();
            this.pos = 0;
        }

        public StringReader(char[] chars) {
            this.chars = chars;
            this.pos = 0;
        }

        public void skipWhitespace() {
            while (canRead()) {
                char c = chars[pos];
                if (c == '\t' || c == ' ' || c == '\r' || c == '\n') {
                    pos++;
                } else break;
            }
        }

        public char read() {
            return chars[pos++];
        }

        public void skip() {
            pos++;
        }

        public void skip(int i) {
            pos+=i;
        }

        public char peek() {
            return chars[pos];
        }

        public int getPos() {
            return pos;
        }

        public int getLength() {
            return chars.length;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public boolean canRead() {
            return pos < chars.length-1;
        }
    }
}
