package com.softwaretree.jdxandroidautoincrementexample;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import com.softwaretree.jdx.JDXHelper;
import com.softwaretree.jdx.JDXS;
import com.softwaretree.jdx.JDXSetup;
import com.softwaretree.jdxandroid.DatabaseAndJDX_Initializer;
import com.softwaretree.jdxandroid.Utils;
import com.softwaretree.jdxandroidautoincrementexample.model.Employee;
import com.softwaretree.jx.JXResource;
import com.softwaretree.jx.JXSession;
import com.softwaretree.jx.JXUtilities;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This project exemplifies how JDXA ORM and associated utilities can be used to easily develop 
 * an Android application that exchanges data of domain model objects with an SQLite database.
 * In particular, this project demonstrates the following:
 * <p>
 * 1) How an ORM Specification (mapping) for domain model classes can be defined textually using
 * simple statements.  The mapping is specified in a text file \res\raw\autoincrement_example.jdx identified 
 * by the resource id R.raw.autoincrement_example.
 * <p>
 * 2) The mapping file shows how an autoincrement primary key attribute may be defined declaratively.
 * <br><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>SQLMAP FOR id COLUMN_NAME EmpId SQLTYPE 'INTEGER PRIMARY KEY AUTOINCREMENT'</code>
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>RDBMS_GENERATED id</code>
 * <p>
 * 3) Use of {@link AppSpecificJDXSetup} and {@link DatabaseAndJDX_Initializer} class to easily: 
 *   <br>&nbsp;&nbsp;&nbsp;&nbsp;  a) create the underlying database, if not already present.
 *   <br>&nbsp;&nbsp;&nbsp;&nbsp;  b) create the schema (tables and constraints) corresponding to the JDXA ORM specification 
 *      the very first time an application runs; the subsequent runs will reuse the 
 *      existing database schema and data. 
 *   <br>&nbsp;&nbsp;&nbsp;&nbsp;  c) populate the schema with application objects data.
 * <p>
 * 4) Examples of how just a few lines of object-oriented code incorporating JDX APIs 
 * can be used to easily interact with relational data.  This avoids tedious and 
 * time-consuming coding/maintenance of low-level SQL statements.
 * Notice that the 'id' field of an Employee object is not initialized in its constructor
 * {@link Employee#Employee(String, java.util.Date, boolean, float)}.  
 * <p> 
 * 5) Examples of how details of an object or a list of objects can be added in JDX log and 
 * how that output can be collected in a file and then displayed in a scrollable TextBox. 
 * <p>
 * @author Damodar Periwal
 */
public class JDXAndroidAutoIncrementExampleActivity extends Activity {
    JDXSetup jdxSetup = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.setTitle(getResources().getString(R.string.activity_title));

        try {       
            AppSpecificJDXSetup.initialize();  // must be done before calling getInstance()
            jdxSetup = AppSpecificJDXSetup.getInstance(this);
                 
            TextView tvJDXLog = (TextView) findViewById(R.id.tvJDXLog);
            tvJDXLog.setMovementMethod(new ScrollingMovementMethod());
            
            // Use a JDXHelper object to capture JDX log output 
            JDXHelper jdxHelper = new JDXHelper(jdxSetup);
            String jdxLogFileName = getFilesDir() + System.getProperty("file.separator") + "jdx.log";
            jdxHelper.setJDXLogging(jdxLogFileName);
    		
            useJDXORM(jdxSetup);
            
            // Show the captured JDX log on the screen
            tvJDXLog.setText(Utils.getTextFileContents(jdxLogFileName));           
        } catch (Exception ex) {
            Toast.makeText(getBaseContext(), "Exception: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            cleanup();
            return;
        }
    }

    /**
     * Do the necessary cleanup.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }
    
    private void cleanup() {
        AppSpecificJDXSetup.cleanup(); // Do this when the application is exiting.
        jdxSetup = null;
    }

    /**
     * Shows some simple examples of using JDXA ORM APIs to exchange object data with a relational database.
     * 
     * @param jdxSetup
     * @throws Exception
     */
    private void useJDXORM(JDXSetup jdxSetup) throws Exception {

        if (null == jdxSetup) {
            return;
        }

        // Obtain ORM handles.
        JXResource jxResource = jdxSetup.checkoutJXResource();
        JXSession jxSessionHandle = jxResource.getJXSessionHandle(); // May be used for creating transaction scopes
        JDXS jdxHandle = jxResource.getJDXHandle();

        String employeeClassName = Employee.class.getName();
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd");
        dfm.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        try {       	
        	// First get the current maximum value of the auto generated id value in the database.
            Integer maxId = (Integer) jdxHandle.getAggregate(employeeClassName, "id", JDXS.AGGR_MAX, null, 0, null);
            JXUtilities.log("\n-- Last max id in the database is " + maxId + " --\n"); 
            
            // Now delete all existing employees from the database.
        	JXUtilities.log("\n-- First deleting all the existing Employee objects from the database --\n"); 
            jdxHandle.delete2(employeeClassName, null, 0);
            
            JXUtilities.log("\n-- Creating and saving two new Employee objects (Mark and Bill) in the database --\n");
            // Create and save a new employee Mark
            Employee emp = new Employee("Mark", dfm.parse("1981-01-01"), true, (float) 51001);
            // By the way, JDXS.IFLAG_IWRGPV is shorter but equivalent to JDXS.IFLAG_INIT_WITH_RDBMS_GENERATED_PKEY_VALUE
            jdxHandle.insert(emp, JDXS.IFLAG_INIT_WITH_RDBMS_GENERATED_PKEY_VALUE, null);
            
            JXUtilities.log("\n-- Displaying the Employee (Mark) object with its pkey (id) initialized with the database generated value after inserting it in the database --\n");
            JXUtilities.printObject(emp, 0, null);

            // Create and save a new employee Bill. 
            // Although the database will generate and save a new id for this Employee but the pkey (id) of 
            // this employee object is not auto-initialized by JDXA with the database generated value. 
            // This gives better performance because a database call to get the generated id is avoided. 
            // Recommended if the generated id is not useful at this stage of the application.
            emp = new Employee("Bill", dfm.parse("1982-02-02"), false, (float) 52002);
            jdxHandle.insert(emp, 0, null);
            
            JXUtilities.log("\n-- Displaying the Employee (Bill) object with its pkey (id) not initialized with the database generated value after inserting it in the database --\n");
            JXUtilities.printObject(emp, 0, null);

            // Retrieve all the employees
            JXUtilities.log("\n-- Querying all the Employee objects --\n");
            List queryResults = jdxHandle.query(employeeClassName, null, JDXS.ALL, JDXS.FLAG_SHALLOW, null);
            JXUtilities.printQueryResults(queryResults);

            // Retrieve employee Bill (name='Bill')
            JXUtilities.log("\n-- Query for the Employee (name='Bill') --\n");
            queryResults = jdxHandle.query(employeeClassName, "name='Bill'", 1, JDXS.FLAG_SHALLOW, null);
            if (queryResults.size() == 1) {
                emp = (Employee) queryResults.get(0);
                JXUtilities.printObject(emp, 0, null);
            }           

            // Change and update attributes of Bill
            JXUtilities.log("\n-- Updating Employee Bill  --\n");
            emp.setExempt(true);
            emp.setCompensation((float) 52002.02);
            jdxHandle.update(emp, 0, null);
            
            // Create and save a new employee Steve
            JXUtilities.log("\n-- Creating a saving a new Employee Steve in the database --\n");
            emp = new Employee("Steve", dfm.parse("1983-03-03"), false, (float) 53003);
            jdxHandle.insert(emp, 0, null);

            // Retrieve all the employees
            JXUtilities.log("\n-- Querying all the Employee objects --\n");
            queryResults = jdxHandle.query(employeeClassName, "order by id", JDXS.ALL, JDXS.FLAG_SHALLOW, null);
            JXUtilities.printQueryResults(queryResults);
            
            // Create and save a new employee Mary with an app supplied id
            JXUtilities.log("\n-- Creating a saving a new Employee Mary in the database --\n");
            emp = new Employee("Mary", dfm.parse("1984-04-04"), false, (float) 54004);
            emp.setId(555);
            jdxHandle.insert(emp, JDXS.IFLAG_OVERRIDE_RDBMS_GENERATED_PKEY_VALUE, null);
            
            // Retrieve all the employees
            JXUtilities.log("\n-- Querying all the Employee objects --\n");
            queryResults = jdxHandle.query(employeeClassName, "order by id", JDXS.ALL, JDXS.FLAG_SHALLOW, null);
            JXUtilities.printQueryResults(queryResults);

        } catch (Exception ex) {
            System.out.println("JDX Error " + ex.getMessage());
            Log.e("JDX", "Error", ex);
            throw ex;
        } finally {
            jdxSetup.checkinJXResource(jxResource);
        }

        return;
    }
}