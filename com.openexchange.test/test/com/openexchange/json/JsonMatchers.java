package com.openexchange.json;

import static com.jayway.jsonassert.JsonAssert.with;
 
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
 
public class JsonMatchers {
 
  public static <T> Matcher<String> hasJsonPath(
    final String jsonPath, final Matcher<T> matches) {
 
    return new TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(String json) {
        try {
          with(json).assertThat(jsonPath, matches);
          return true;
        } catch (AssertionError ae) {
          return false;
        } catch (Exception e) {
          return false;
        }
      }
 
      @Override
      public void describeTo(Description description) {
        description
          .appendText(" JSON object with a value at node ")
          .appendValue(jsonPath)
          .appendText(" that is ")
          .appendDescriptionOf(matches);
      }
    };
  }
}