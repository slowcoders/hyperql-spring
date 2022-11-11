package org.slowcoders.jql.parser;

import java.util.Collection;

public class SourceWriter<Self extends SourceWriter> {
    private final StringBuilder sb = new StringBuilder();
    private final char quote;
    private int tab;

    public SourceWriter(char quote) {
        this.quote = quote;
    }

    public Self writeln(String text) {
        this.write(text);
        this.writeln();
        return (Self)this;
    }

    public Self incTab() {
        this.tab ++;
        return (Self)this;
    }

    public Self decTab() {
        if (this.tab > 0) {
            this.tab--;
        }
        return (Self)this;
    }

    public Self write(char ch) {
        char last_ch = sb.length() == 0 ? 0 : sb.charAt(sb.length()-1);
        if (last_ch == '\n') {
            for (int t = tab; --t >= 0; ) {
                sb.append('\t');
            }
        }
        sb.append(ch);
        return (Self)this;
    }

    public Self write(String text) {
        for (int i = 0; i < text.length(); i ++) {
            char ch = text.charAt(i);
            write(ch);
        }
        return (Self)this;
    }

    public Self writeRaw(String text) {
        sb.append(text);
        return (Self)this;
    }

    public void writeln() {
        sb.append('\n');
    }

    public Self writeF(String format, String... params) {
        for (int i = 0; i < format.length(); i ++) {
            char ch = format.charAt(i);
            if (ch != '{') {
                write(ch);
                continue;
            }

            int idx = 0;
            while (true) {
                ch = format.charAt(++i);
                if (ch >= 0 && ch <= '9') {
                    idx = idx * 10 + ch - '0';
                }
                if (ch == '}') break;
            }
            write(params[idx]);
        }
        return (Self)this;
    }

    public Self writeQuoted(Object value) {
        sb.append(quote).append(value).append(quote);
        return (Self)this;
    }

    public Self writeValue(Object v) {
        return isQuotedColumn(v) ? writeQuoted(v) : writeRaw(String.valueOf(v));
    }

    public Self writeValues(Collection values) {
        if (values == null || values.size() == 0) {
            return (Self)this;
        }

        for (Object v : values) {
            writeValue(v);
            sb.append(", ");
        }
        sb.setLength(sb.length() - 2);
        return (Self)this;
    }

    protected boolean isQuotedColumn(Object value) {
        if (value == null) return false;
        Class<?> type = value.getClass();
        return !Number.class.isAssignableFrom(type) &&
                !Boolean.class.isAssignableFrom(type);
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public Self shrinkLength(int len) {
        sb.setLength(sb.length() - len);
        return (Self)this;
    }

    public void clear() {
        sb.setLength(0);
    }

    public Self replaceTrailingComma(String text) {
        int len = sb.length();
        while (len > 0) {
            char ch = sb.charAt(--len);
            if (ch <= ' ') continue;
            if (ch == ',') break;
        }
        sb.setLength(len);
        this.write(text);
        return (Self)this;
    }

}