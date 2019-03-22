package serde

import com.klarna.hiverunner.HiveShell
import com.klarna.hiverunner.StandaloneHiveRunner
import com.klarna.hiverunner.annotations.HiveSQL
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(StandaloneHiveRunner.class)
class HiveSpec {
  @HiveSQL(files = [], autoStart = false)
  HiveShell shell

  @Before
  void setup() {
    shell.start()
  }

  @Test
  void test() {
    given:
    shell.execute("create database test_db;")
    shell.execute("use test_db;")
    shell.execute("""
CREATE TABLE my_table(a string, b string, c float)
ROW FORMAT SERDE 'serde.CustomSerDe'
WITH SERDEPROPERTIES (
   "separatorChar" = ",",
   "quoteChar" = "'"
)
STORED AS INPUTFORMAT "serde.CsvInputFormat" OUTPUTFORMAT "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
location "/home/sudhir.singh/Development/IdeaProjects/sample/sample";
""")
    when:
    List result = shell.executeQuery("select a,c from my_table")
    then:
    result.size() == 300
  }

}



