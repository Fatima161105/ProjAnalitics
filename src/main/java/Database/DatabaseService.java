package Database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DatabaseService {
    private final Dao<GroupEntity, Integer> groupDao;
    private final Dao<StudentEntity, Integer> studentDao;
    private final Dao<GradeEntity, Integer> gradeDao;
    private final Dao<TopicEntity, Integer> topicDao;

    public DatabaseService(DatabaseHelper dbHelper) throws SQLException {
        groupDao = DaoManager.createDao(dbHelper.getConnectionSource(), GroupEntity.class);
        studentDao = DaoManager.createDao(dbHelper.getConnectionSource(), StudentEntity.class);
        gradeDao = DaoManager.createDao(dbHelper.getConnectionSource(), GradeEntity.class);
        topicDao = DaoManager.createDao(dbHelper.getConnectionSource(), TopicEntity.class);

    }

    public GroupEntity addOrGetGroup(String groupName) throws SQLException {
        GroupEntity group = groupDao.queryBuilder()
                .where().eq("name", groupName)
                .queryForFirst();

        if (group == null) {
            group = new GroupEntity(groupName);
            groupDao.create(group);
        }
        return group;
    }

    public StudentEntity addStudent(StudentEntity student) throws SQLException {
        studentDao.create(student);
        return student;
    }

    public GradeEntity addGrade(StudentEntity student, TopicEntity topic, int gradeValue) throws SQLException {
        GradeEntity grade = new GradeEntity(student, topic, gradeValue);
        gradeDao.create(grade);
        return grade;
    }

    public TopicEntity addOrGetTopic(String name) throws SQLException {
        TopicEntity topic = topicDao.queryBuilder()
                .where().eq("name", name)
                .queryForFirst();

        if (topic == null) {
            topic = new TopicEntity(name);
            topicDao.create(topic);
        }
        return topic;
    }


    public List<TopicEntity> getAllTopics() throws SQLException {
        return topicDao.queryForAll();
    }
    public List<GroupEntity> getAllGroups() throws SQLException {
        return groupDao.queryForAll();
    }
    public List<StudentEntity> getAllStudents() throws SQLException {
        return studentDao.queryForAll();
    }
    public List<GradeEntity> getAllGrades() throws SQLException {
        return gradeDao.queryForAll();
    }


    public Map<String, Map<String, Double>> getGradesByGroupAndTopic() throws SQLException {
        List<GroupEntity> groups = groupDao.queryForAll(); // Получаем все группы
        List<TopicEntity> topics = topicDao.queryForAll(); // Получаем все темы
        Map<String, Map<String, Double>> groupGrades = new HashMap<>(); // Хранение данных

        for (GroupEntity group : groups) {
            Map<String, Double> topicAverages = new HashMap<>(); // Средние по каждой теме для текущей группы

            for (TopicEntity topic : topics) {
                // Получаем список оценок для текущей группы и темы
                List<GradeEntity> grades = gradeDao.queryBuilder()
                        .join(studentDao.queryBuilder().where().eq("group_id", group.getId()).queryBuilder()) // Соединяем с таблицей студентов по group_id
                        .join(topicDao.queryBuilder().where().eq("id", topic.getId()).queryBuilder()) // Соединяем с таблицей тем по id
                        .query();

                // Вычисляем среднюю оценку для данной темы в группе
                double averageGrade = grades.stream()
                        .mapToInt(GradeEntity::getGradeValue)
                        .average()
                        .orElse(0.0);

                topicAverages.put(topic.getName(), averageGrade); // Сохраняем среднюю оценку по теме
            }

            groupGrades.put(group.getName(), topicAverages); // Сохраняем данные по группе
        }

        return groupGrades;
    }
    public Map<String, Map<String, Double>> getStudentGradesByGroup(String groupName) throws SQLException {
        // Получаем группу
        GroupEntity group = groupDao.queryBuilder()
                .where().eq("name", groupName)
                .queryForFirst();

        if (group == null) {
            throw new SQLException("Группа не найдена: " + groupName);
        }

        // Получаем студентов группы
        List<StudentEntity> students = studentDao.queryBuilder()
                .where().eq("group_id", group.getId())
                .query();

        Map<String, Map<String, Double>> result = new HashMap<>();

        for (StudentEntity student : students) {
            Map<String, Double> grades = new HashMap<>();

            // Средняя оценка за ДЗ
            double dzAverage = gradeDao.queryBuilder()
                    .join(topicDao.queryBuilder().where().eq("name", "ДЗ").queryBuilder())
                    .where().eq("student_id", student.getId())
                    .query()
                    .stream()
                    .mapToInt(GradeEntity::getGradeValue)
                    .average()
                    .orElse(0.0);

            // Средняя оценка за Упражнение
            double exerciseAverage = gradeDao.queryBuilder()
                    .join(topicDao.queryBuilder().where().eq("name", "Упражнение").queryBuilder())
                    .where().eq("student_id", student.getId())
                    .query()
                    .stream()
                    .mapToInt(GradeEntity::getGradeValue)
                    .average()
                    .orElse(0.0);

            grades.put("ДЗ", dzAverage);
            grades.put("Упражнение", exerciseAverage);

            result.put(student.getName(), grades);
        }

        return result;
    }
}