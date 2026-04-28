package Database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "grades")
public class GradeEntity {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private StudentEntity student;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private TopicEntity topic;

    @DatabaseField(canBeNull = false)
    private int gradeValue;

    // Конструктор по умолчанию для ORMLite
    public GradeEntity() {}

    // Конструктор
    public GradeEntity(StudentEntity student, TopicEntity topic, int gradeValue) {
        this.student = student;
        this.topic = topic;
        this.gradeValue = gradeValue;
    }

    // Геттеры и сеттеры
    public int getGradeValue() {
        return gradeValue;
    }

    public void setGradeValue(int gradeValue) {
        this.gradeValue = gradeValue;
    }
}
