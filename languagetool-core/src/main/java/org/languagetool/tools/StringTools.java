/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.tools;

import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Tools for working with strings.
 * 
 * @author Daniel Naber
 */
public final class StringTools {

  /**
   * Constants for printing XML rule matches.
   */
  public static enum XmlPrintMode {
    /**
     * Normally output the rule matches by starting and
     * ending the XML output on every call.
     */
    NORMAL_XML,
    /**
     * Start XML output by printing the preamble and the
     * start of the root element.
     */
    START_XML,
    /**
     * End XML output by closing the root element.
     */
    END_XML,
    /**
     * Simply continue rule match output.
     */
    CONTINUE_XML
  }

  private static final int DEFAULT_CONTEXT_SIZE = 25;
  private static final Pattern XML_COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern XML_PATTERN = Pattern.compile("(?<!<)<[^<>]+>", Pattern.DOTALL);

  private StringTools() {
    // only static stuff
  }

  /**
   * Throw exception if the given string is null or empty or only whitespace.
   */
  public static void assureSet(final String s, final String varName) {
    Objects.requireNonNull(varName);
    if (isEmpty(s.trim())) {
      throw new IllegalArgumentException(varName + " cannot be empty or whitespace only");
    }
  }

  /**
   * Read a file's content.
   * @deprecated use {@link #readStream(java.io.InputStream, String)} instead (deprecated since LT 2.3)
   */
  public static String readFile(final InputStream file) throws IOException {
    return readFile(file, null);
  }

  /**
   * Read the text stream using the given encoding.
   * @deprecated use {@link #readStream(java.io.InputStream, String)} instead (deprecated since LT 2.3)
   */
  public static String readFile(final InputStream stream, final String encoding) throws IOException {
    return readStream(stream, encoding);
  }

  /**
   * Read the text stream using the given encoding.
   *
   * @param stream InputStream the stream to be read
   * @param encoding the stream's character encoding, e.g. {@code utf-8}, or {@code null} to use the system encoding
   * @return a string with the stream's content, lines separated by {@code \n} (note that {@code \n} will
   *  be added to the last line even if it is not in the stream)
   * @throws IOException
   * @since LanguageTool 2.3
   */
  public static String readStream(final InputStream stream, final String encoding) throws IOException {
    InputStreamReader isr = null;
    BufferedReader br = null;
    final StringBuilder sb = new StringBuilder();
    try {
      if (encoding == null) {
        isr = new InputStreamReader(stream);
      } else {
        isr = new InputStreamReader(stream, encoding);
      }
      br = new BufferedReader(isr);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
    } finally {
      if (br != null) {
        br.close();
      }
      if (isr != null) {
        isr.close();
      }
    }
    return sb.toString();
  }

  /**
   * Returns true if the given string is made up of all-uppercase characters
   * (ignoring characters for which no upper-/lowercase distinction exists).
   */
  public static boolean isAllUppercase(final String str) {
    for(int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isLetter(c) && Character.isLowerCase(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the given string is mixed case, like {@code MixedCase} or {@code mixedCase}
   * (but not {@code Mixedcase}).
   * @param str input str
   */
  public static boolean isMixedCase(final String str) {
    return !isAllUppercase(str)
        && !isCapitalizedWord(str)
        && isNotAllLowercase(str);
  }

  /**
   * Returns true if <code>str</code> is made up of all-lowercase characters
   * (ignoring characters for which no upper-/lowercase distinction exists).
   * @since 2.5
   */
  public static boolean isNotAllLowercase(final String str) {
    for(int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isLetter(c) && !Character.isLowerCase(c)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param str input string
   * @return true if word starts with an uppercase letter and all other letters are lowercase
   */
  public static boolean isCapitalizedWord(final String str) {
    if (!isEmpty(str) && Character.isUpperCase(str.charAt(0))) {
      for (int i = 1; i < str.length(); i++) {
        char c = str.charAt(i);
        if (Character.isLetter(c) && !Character.isLowerCase(c)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Whether the first character of <code>str</code> is an uppercase character.
   */
  public static boolean startsWithUppercase(final String str) {
    if (isEmpty(str)) {
      return false;
    }
    return Character.isUpperCase(str.charAt(0));
  }

  /**
   * Return <code>str</code> modified so that its first character is now an
   * uppercase character. If <code>str</code> starts with non-alphabetic
   * characters, such as quotes or parentheses, the first character is 
   * determined as the first alphabetic character.
   */
  public static String uppercaseFirstChar(final String str) {
    return changeFirstCharCase(str, true);
  }

  /**
   * Return <code>str</code> modified so that its first character is now an
   * lowercase character. If <code>str</code> starts with non-alphabetic
   * characters, such as quotes or parentheses, the first character is 
   * determined as the first alphabetic character.
   */
  public static String lowercaseFirstChar(final String str) {
    return changeFirstCharCase(str, false);
  }

  /**
   * Return <code>str</code> modified so that its first character is now an
   * lowercase or uppercase character, depending on <code>toUpperCase</code>.
   * If <code>str</code> starts with non-alphabetic
   * characters, such as quotes or parentheses, the first character is 
   * determined as the first alphabetic character.
   */
  private static String changeFirstCharCase(final String str, final boolean toUpperCase) {
    if (isEmpty(str)) {
      return str;
    }
    if (str.length() == 1) {
      return toUpperCase ? str.toUpperCase(Locale.ENGLISH) : str.toLowerCase();
    }
    int pos = 0;
    final int len = str.length() - 1;
    while (!Character.isLetterOrDigit(str.charAt(pos)) && len > pos) {
      pos++;
    }
    final char firstChar = str.charAt(pos);    
    return str.substring(0, pos) 
        + (toUpperCase ? Character.toUpperCase(firstChar) : Character.toLowerCase(firstChar))
        + str.substring(pos + 1);
  }

  public static String readerToString(final Reader reader) throws IOException {
    final StringBuilder sb = new StringBuilder();
    int readBytes = 0;
    final char[] chars = new char[4000];
    while (readBytes >= 0) {
      readBytes = reader.read(chars, 0, 4000);
      if (readBytes <= 0) {
        break;
      }
      sb.append(new String(chars, 0, readBytes));
    }
    return sb.toString();
  }

  /**
   * @deprecated use {@link #streamToString(java.io.InputStream, String)} instead (deprecated since 1.8)
   */
  public static String streamToString(final InputStream is) throws IOException {
    try (InputStreamReader isr = new InputStreamReader(is)) {
      return readerToString(isr);
    }
  }

  public static String streamToString(final InputStream is, String charsetName) throws IOException {
    try (InputStreamReader isr = new InputStreamReader(is, charsetName)) {
      return readerToString(isr);
    }
  } 
  
  /**
   * Calls {@link #escapeHTML(String)}.
   */
  public static String escapeXML(final String s) {
    return escapeHTML(s);
  }

  /**
   * Escapes these characters: less than, greater than, quote, ampersand.
   */
  public static String escapeHTML(final String s) {
    // this version is much faster than using s.replaceAll()
    final StringBuilder sb = new StringBuilder();
    final int n = s.length();
    for (int i = 0; i < n; i++) {
      final char c = s.charAt(i);
      switch (c) {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '"':
          sb.append("&quot;");
          break;

        default:
          sb.append(c);
        break;
      }
    }
    return sb.toString();
  }

  /**
   * Get an XML representation of the given rule matches.
   * 
   * @param text the original text that was checked, used to get the context of the matches
   * @param contextSize the desired context size in characters
   * @deprecated Use {@link #ruleMatchesToXML(List,String,int,XmlPrintMode)} instead (deprecated since ~ 1.0)
   */
  public static String ruleMatchesToXML(final List<RuleMatch> ruleMatches,
      final String text, final int contextSize) {
    return ruleMatchesToXML(ruleMatches, text, contextSize, XmlPrintMode.NORMAL_XML);
  }

  /**
   * Get an XML representation of the given rule matches.
   *
   * @param text the original text that was checked, used to get the context of the matches
   * @param contextSize the desired context size in characters
   * @param xmlMode how to print the XML
   * @param lang the language of the text (might be null)
   * @param motherTongue the mother tongue of the user (might be null)
   * @deprecated Use {@link RuleAsXmlSerializer} instead (deprecated since 2.5)
   */
  public static String ruleMatchesToXML(final List<RuleMatch> ruleMatches,
      final String text, final int contextSize, final XmlPrintMode xmlMode,
      final Language lang, final Language motherTongue) {
    final StringBuilder xml = new StringBuilder(200);
    RuleAsXmlSerializer serializer = new RuleAsXmlSerializer();
    if (xmlMode == XmlPrintMode.NORMAL_XML || xmlMode == XmlPrintMode.START_XML) {
      xml.append(serializer.getXmlStart(lang, motherTongue));
    }
    xml.append(serializer.ruleMatchesToXmlSnippet(ruleMatches, text, contextSize));
    if (xmlMode == XmlPrintMode.NORMAL_XML || xmlMode == XmlPrintMode.END_XML) {
      xml.append(serializer.getXmlEnd());
    }
    return xml.toString();
  }

  /**
   * Get an XML representation of the given rule matches.
   *
   * @param text the original text that was checked, used to get the context of the matches
   * @param contextSize the desired context size in characters
   * @param xmlMode how to print the XML
   * @deprecated Use {@link RuleAsXmlSerializer} instead (deprecated since 2.5)
   */
  public static String ruleMatchesToXML(final List<RuleMatch> ruleMatches,
      final String text, final int contextSize, final XmlPrintMode xmlMode) {
    return ruleMatchesToXML(ruleMatches, text, contextSize, xmlMode, null, null);
  }

  public static String listToString(final Collection<String> l, final String delimiter) {
    final StringBuilder sb = new StringBuilder();
    for (final Iterator<String> iter = l.iterator(); iter.hasNext();) {
      final String str = iter.next();
      sb.append(str);
      if (iter.hasNext()) {
        sb.append(delimiter);
      }
    }
    return sb.toString();
  }

  /**
   * @deprecated use {@link ContextTools#getPlainTextContext(int, int, String)} instead (deprecated since LanguageTool 2.3)
   */
  public static String getContext(final int fromPos, final int toPos,
      final String contents) {
    final ContextTools contextTools = new ContextTools();
    contextTools.setContextSize(DEFAULT_CONTEXT_SIZE);
    return contextTools.getPlainTextContext(fromPos, toPos, contents);
  }

  /**
   * @deprecated use {@link ContextTools#getPlainTextContext(int, int, String)} instead (deprecated since LanguageTool 2.3)
   */
  public static String getContext(final int fromPos, final int toPos,
      final String contents, final int contextSize) {
    final ContextTools contextTools = new ContextTools();
    contextTools.setContextSize(contextSize);
    return contextTools.getPlainTextContext(fromPos, toPos, contents);
  }

  /**
   * Filters any whitespace characters. Useful for trimming the contents of
   * token elements that cannot possibly contain any spaces.
   * 
   * @param str String to be filtered.
   * @return Filtered string.
   */
  public static String trimWhitespace(final String str) {
    final StringBuilder filter = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);
      if (c != '\n' && c != ' ' && c != '\t' && c != '\r') {
        filter.append(c);
      }
    }
    return filter.toString();
  }

  /**
   * Adds spaces before words that are not punctuation.
   * 
   * @param word Word to add the preceding space.
   * @param language
   *          Language of the word (to check typography conventions). Currently
   *          French convention of not adding spaces only before '.' and ',' is
   *          implemented; other languages assume that before ,.;:!? no spaces
   *          should be added.
   * @return String containing a space or an empty string.
   */
  public static String addSpace(final String word, final Language language) {
    String space = " ";
    if (word.length() == 1) {
      final char c = word.charAt(0);
      if ("fr".equals(language.getShortName())) {
        if (c == '.' || c == ',') {
          space = "";
        }
      } else {
        if (c == '.' || c == ',' || c == ';' || c == ':' || c == '?' || c == '!') {
          space = "";
        }
      }
    }
    return space;
  }

  /**
   * Checks if a string contains only whitespace, including all Unicode
   * whitespace, but not the non-breaking space. This differs a bit from the 
   * definition of whitespace in Java 7 because of the way we want to interpret Khmer.
   * 
   * @param str String to check
   * @return true if the string is whitespace-only
   */
  public static boolean isWhitespace(final String str) {
    if ("\u0002".equals(str) // unbreakable field, e.g. a footnote number in OOo
        || "\u0001".equals(str)) { // breakable field in OOo
      return false;
    }
    final String trimStr = str.trim();
    if (isEmpty(trimStr)) {
      return true;
    }
    if (trimStr.length() == 1) {
      if ("\u200B".equals(str)) {
        // We need u200B​​ to be detected as whitespace for Khmer, as it was the case before Java 7.
        return true;
      }
      return java.lang.Character.isWhitespace(trimStr.charAt(0));
    }
    return false;
  }
  
  /**
   * Checks if a string is the non-breaking whitespace (<code>\u00A0</code>).
   * @since 2.1
   */
  public static boolean isNonBreakingWhitespace(final String str) {
    return "\u00A0".equals(str);
  }

  /**
   * @param ch Character to check
   * @return True if the character is a positive number (decimal digit from 1 to 9).
   */
  public static boolean isPositiveNumber(final char ch) {
    return ch >= '1' && ch <= '9';
  }

  /**
   * Helper method to replace calls to {@code "".equals()}.
   * 
   * @param str String to check
   * @return true if string is empty or {@code null}
   */
  public static boolean isEmpty(final String str) {
    return str == null || str.length() == 0;
  }

  /**
   * Simple XML filtering routing
   * @param str XML string to be filtered.
   * @return Filtered string without XML tags.
   */
  public static String filterXML(final String str) {
    String s = str;       
    s = XML_COMMENT_PATTERN.matcher(s).replaceAll(" ");
    s = XML_PATTERN.matcher(s).replaceAll("");
    return s;
  }

  public static String asString(final CharSequence s) {
    if (s == null) {
      return null;
    }
    return s.toString();
  }
  
  /**
   * Mimicks Java 1.7 {@link Character#isAlphabetic} (needed as we require only Java 1.6)
   *  
   * @param codePoint The input character.
   * @return True if the character is a Unicode alphabetic character.
   */
  public static boolean isAlphabetic(int codePoint) {
    return (((((1 << Character.UPPERCASE_LETTER) |
        (1 << Character.LOWERCASE_LETTER) |
        (1 << Character.TITLECASE_LETTER) |
        (1 << Character.MODIFIER_LETTER) |
        (1 << Character.OTHER_LETTER) |
        (1 << Character.LETTER_NUMBER)) >> Character.getType(codePoint)) & 1) != 0);
  }

}
