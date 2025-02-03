package com.appiancorp.solutionsconsulting.plugins.delimfiletools.expressions;

import com.appiancorp.suiteapi.expression.annotations.Category;

import java.lang.annotation.*;

@Category("delimFileToolsCategory")
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DelimFileToolsCategory {

}

