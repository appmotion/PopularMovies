/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.appmotion.popularmovies.data;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * These are functions and some test data to make it easier to test your database and Content
 * Provider.
 */
class TestUtilities {

  /* October 1st, 2016 at midnight, GMT time */
  static final long DATE_NORMALIZED = 1475280000000L;

  static final int BULK_INSERT_RECORDS_TO_INSERT = 10;

  /**
   * Ensures there is a non empty cursor and validates the cursor's data by checking it against
   * a set of expected values. This method will then close the cursor.
   *
   * @param error Message when an error occurs
   * @param valueCursor The Cursor containing the actual values received from an arbitrary query
   * @param expectedValues The values we expect to receive in valueCursor
   */
  static void validateThenCloseCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
    assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
    validateCurrentRecord(error, valueCursor, expectedValues);
    valueCursor.close();
  }

  /**
   * This method iterates through a set of expected values and makes various assertions that
   * will pass if our app is functioning properly.
   *
   * @param error Message when an error occurs
   * @param valueCursor The Cursor containing the actual values received from an arbitrary query
   * @param expectedValues The values we expect to receive in valueCursor
   */
  static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
    Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();

    for (Map.Entry<String, Object> entry : valueSet) {
      String columnName = entry.getKey();
      int index = valueCursor.getColumnIndex(columnName);

      /* Test to see if the column is contained within the cursor */
      String columnNotFoundError = "Column '" + columnName + "' not found. " + error;
      assertFalse(columnNotFoundError, index == -1);

      /* Test to see if the expected value equals the actual value (from the Cursor) */
      String expectedValue = entry.getValue().toString();
      String actualValue = valueCursor.getString(index);

      String valuesDontMatchError = "Actual value '" + actualValue + "' did not match the expected value '" + expectedValue + "'. " + error;

      assertEquals(valuesDontMatchError, expectedValue, actualValue);
    }
  }

  static String getStaticStringField(Class clazz, String variableName) throws NoSuchFieldException, IllegalAccessException {
    Field stringField = clazz.getDeclaredField(variableName);
    stringField.setAccessible(true);
    String value = (String) stringField.get(null);
    return value;
  }

  static Integer getStaticIntegerField(Class clazz, String variableName) throws NoSuchFieldException, IllegalAccessException {
    Field intField = clazz.getDeclaredField(variableName);
    intField.setAccessible(true);
    Integer value = (Integer) intField.get(null);
    return value;
  }

  static String studentReadableClassNotFound(ClassNotFoundException e) {
    String message = e.getMessage();
    int indexBeforeSimpleClassName = message.lastIndexOf('.');
    String simpleClassNameThatIsMissing = message.substring(indexBeforeSimpleClassName + 1);
    simpleClassNameThatIsMissing = simpleClassNameThatIsMissing.replaceAll("\\$", ".");
    String fullClassNotFoundReadableMessage =
        "Couldn't find the class " + simpleClassNameThatIsMissing + ".\nPlease make sure you've created that class and followed the TODOs.";
    return fullClassNotFoundReadableMessage;
  }

  static String studentReadableNoSuchField(NoSuchFieldException e) {
    String message = e.getMessage();

    Pattern p = Pattern.compile("No field (\\w*) in class L.*/(\\w*\\$?\\w*);");

    Matcher m = p.matcher(message);

    if (m.find()) {
      String missingFieldName = m.group(1);
      String classForField = m.group(2).replaceAll("\\$", ".");
      String fieldNotFoundReadableMessage = "Couldn't find "
          + missingFieldName
          + " in class "
          + classForField
          + "."
          + "\nPlease make sure you've declared that field and followed the TODOs.";
      return fieldNotFoundReadableMessage;
    } else {
      return e.getMessage();
    }
  }
}