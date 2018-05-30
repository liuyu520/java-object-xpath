package com.kunlunsoft.util.objectxpath;

import com.kunlunsoft.util.objectxpath.bean.Student;
import com.kunlunsoft.util.objectxpath.bean.Teacher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyTest {
    /***
     * 2018/5/31
     */
    @org.junit.Test
    public final void test_xpath() {
        Teacher teacher = new Teacher();
        List<Student> students = new ArrayList<>();
        Student student = new Student();
        student.setId(1);
        student.setName("付攀");
        student.setAge(20);
        students.add(student);

        student = new Student();
        student.setId(2);
        student.setName("熊雕");
        student.setAge(22);
        students.add(student);


        teacher.setTitle("教授");
        teacher.setName("陈定生");
        teacher.setStudents(students);


        final ObjectXpath oxp = new ObjectXpath();
        System.out.println(" :" + oxp.process("/students[0]/@name", teacher));
        assertEquals("付攀", oxp.process("/students[0]/@name", teacher));
    }
}
