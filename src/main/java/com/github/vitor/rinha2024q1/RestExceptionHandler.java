package com.github.vitor.rinha2024q1;

import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleAllExceptions(Exception e) {
    	e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    
    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<Void> handleUncategorizedSQLException(UncategorizedSQLException e) {
    	SQLException sqlEx = e.getSQLException();
    	
    	switch (sqlEx.getSQLState()) {
	    	case "90422": {
	    		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
	    	}
	    	case "90404": {
	    		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	    	}
    	}

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}