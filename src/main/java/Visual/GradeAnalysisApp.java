package Visual;
import Database.DatabaseHelper;
import Database.DatabaseService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GradeAnalysisApp {

    private static DatabaseService dbService;

    public static void main(String[] args) {
        try {
            // Инициализация DatabaseHelper и DatabaseService
            DatabaseHelper dbHelper = new DatabaseHelper();
            dbService = new DatabaseService(dbHelper);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка инициализации базы данных.");
            return;
        }

        // Создание и отображение UI
        SwingUtilities.invokeLater(GradeAnalysisApp::createAndShowUI);
    }
    public static void createAndShowUI() {
        JFrame frame = new JFrame("Анализ оценок");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1300, 900);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20)); // Центрируем кнопки и добавляем отступы

        // Создаем кнопки
        JButton analyzeButton = new JButton("Построить график");
        JButton generateReportButton = new JButton("Создать отчет в Excel");

        // Настройка кнопок (цвет, размер, шрифт)
        analyzeButton.setFont(new Font("Arial", Font.BOLD, 24));
        analyzeButton.setForeground(Color.WHITE); // Белый цвет текста
        analyzeButton.setPreferredSize(new Dimension(300, 80));

        generateReportButton.setFont(new Font("Arial", Font.BOLD, 24));
        generateReportButton.setBackground(new Color(34, 193, 195));
        generateReportButton.setForeground(Color.WHITE); // Белый цвет текста
        generateReportButton.setPreferredSize(new Dimension(300, 80));

        // Обработчик кнопки для создания отчета
        generateReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Создаем экземпляр ExcelReportGenerator и генерируем отчет
                    ExcelReportGenerator reportGenerator = new ExcelReportGenerator(dbService);
                    reportGenerator.generateExcelReport();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Ошибка при создании отчета: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Добавляем кнопки на панель
        panel.add(analyzeButton);
        panel.add(generateReportButton);

        // Добавляем панель на фрейм
        frame.getContentPane().add(panel);
        frame.setVisible(true);

        // Обработчик кнопки для построения графика
        analyzeButton.addActionListener(e -> showSortedGradeChart(panel));
    }

    private static void showSortedGradeChart(JPanel panel) {
        try {
            // Получаем средние оценки по группам и темам
            Map<String, Map<String, Double>> data = dbService.getGradesByGroupAndTopic();

            // Подготовка данных для графика
            DefaultCategoryDataset dataset = createSortedDataset(data);

            // Создание графика
            JFreeChart chart = ChartFactory.createBarChart(
                    "Средние оценки по группам (сортировка по убыванию)",
                    "Группа",
                    "Средняя оценка",
                    dataset
            );

            // Настройка отображения графика
            CategoryPlot plot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setDrawBarOutline(false);

            // Включаем подсказки
            renderer.setDefaultToolTipGenerator((dataset1, row, column) -> {
                String groupName = (String) dataset1.getRowKey(row);
                double value = dataset1.getValue(row, column).doubleValue();
                return String.format("Группа: %s, Средний балл: %.2f", groupName, value);
            });

            // Убираем легенду
            chart.removeLegend();

            // Панель для отображения графика
            ChartPanel chartPanel = new ChartPanel(chart);

            // Включаем отображение всплывающих подсказок
            chartPanel.setToolTipText(""); // Активируем подсказки

            // Обработчик для кликов по диаграмме
            chartPanel.addChartMouseListener(new org.jfree.chart.ChartMouseListener() {
                @Override
                public void chartMouseClicked(org.jfree.chart.ChartMouseEvent event) {
                    // При клике на диаграмму отображаем подробную диаграмму для студентов группы
                    org.jfree.chart.entity.ChartEntity entity = event.getEntity();
                    if (entity instanceof org.jfree.chart.entity.CategoryItemEntity) {
                        org.jfree.chart.entity.CategoryItemEntity categoryEntity = (org.jfree.chart.entity.CategoryItemEntity) entity;
                        String groupName = (String) categoryEntity.getRowKey();
                        showStudentGradesChart(groupName);
                    }
                }

                @Override
                public void chartMouseMoved(org.jfree.chart.ChartMouseEvent event) {

                }
            });

            // Обновление панели с графиком
            panel.removeAll();
            panel.add(chartPanel, BorderLayout.CENTER);
            panel.revalidate();
            panel.repaint();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка загрузки данных из базы.");
        }
    }

    private static void showStudentGradesChart(String groupName) {
        try {
            AtomicReference<Map<String, Map<String, Double>>> studentData = new AtomicReference<>(dbService.getStudentGradesByGroup(groupName));

            JFrame studentFrame = new JFrame("Оценки студентов группы: " + groupName);
            studentFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            studentFrame.setSize(1000, 600);

            JPanel mainPanel = new JPanel(new BorderLayout());

            JPanel chartPanel = new JPanel(new BorderLayout());
            DefaultCategoryDataset dataset = createStudentDatasetWithNumbers(studentData.get());
            JFreeChart chart = ChartFactory.createBarChart(
                    "Оценки студентов группы " + groupName,
                    "Номер студента",
                    "Оценка",
                    dataset
            );

            CategoryPlot plot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setDrawBarOutline(false);
            chart.removeLegend();

            ChartPanel studentChartPanel = new ChartPanel(chart);
            chartPanel.add(studentChartPanel, BorderLayout.CENTER);

            // Создание панели с именами студентов
            JPanel namesPanel = new JPanel();
            namesPanel.setLayout(new BoxLayout(namesPanel, BoxLayout.Y_AXIS));

            List<String> studentNames = new ArrayList<>(studentData.get().keySet());
            Map<Integer, String> numberToStudentMap = new HashMap<>();
            JPanel numberingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Панель с нумерацией
            JLabel numberingLabel = new JLabel(); // Метка для отображения нумерации

            // Добавление студентов в список
            for (int i = 0; i < studentNames.size(); i++) {
                int number = i + 1;
                String studentName = studentNames.get(i);
                numberToStudentMap.put(number, studentName);

                JButton studentButton = new JButton(number + ". " + studentName);
                studentButton.addActionListener(e -> highlightStudentBar(chart, number)); // Обработчик выделения столбца
                namesPanel.add(studentButton);
            }

            numberingLabel.setText("Номера: 1 - " + studentNames.size());
            numberingPanel.add(numberingLabel);

            JScrollPane namesScrollPane = new JScrollPane(namesPanel);
            namesScrollPane.setPreferredSize(new Dimension(200, 0));

            JPanel filterPanel = new JPanel(new FlowLayout());
            JButton sortByDzButton = new JButton("Сортировать по ДЗ");
            JButton sortByExerciseButton = new JButton("Сортировать по Упражнению");
            JButton backButton = new JButton("Назад");  // Кнопка "Назад"
            JButton resetFilterButton = new JButton("Сбросить фильтр");

            filterPanel.add(sortByDzButton);
            filterPanel.add(sortByExerciseButton);
            filterPanel.add(resetFilterButton);
            filterPanel.add(backButton); // Добавляем кнопку "Назад"

            // Добавление действия для кнопки "Назад"
            backButton.addActionListener(e -> {
                // Возвращаемся к предыдущему экрану с графиком
                showSortedGradeChart((JPanel) studentFrame.getContentPane());  // Здесь передаем контейнер, на который нужно отобразить график
                studentFrame.dispose();  // Закрываем текущее окно
            });

            sortByDzButton.addActionListener(e -> {
                Map<String, Map<String, Double>> sortedByDz = sortStudentData(studentData.get(), "ДЗ");
                DefaultCategoryDataset newDataset = createStudentDatasetWithNumbers(sortedByDz);
                studentData.set(sortedByDz);
                refreshUI(namesPanel, chartPanel, chart, newDataset, sortedByDz);
            });

            sortByExerciseButton.addActionListener(e -> {
                Map<String, Map<String, Double>> sortedByExercise = sortStudentData(studentData.get(), "Упражнение");
                DefaultCategoryDataset newDataset = createStudentDatasetWithNumbers(sortedByExercise);
                studentData.set(sortedByExercise);
                refreshUI(namesPanel, chartPanel, chart, newDataset, sortedByExercise);
            });

            resetFilterButton.addActionListener(e -> {
                try {
                    // Восстановление данных группы с базы данных
                    Map<String, Map<String, Double>> originalData = dbService.getStudentGradesByGroup(groupName);
                    studentData.set(originalData);
                    DefaultCategoryDataset originalDataset = createStudentDatasetWithNumbers(originalData);
                    refreshUI(namesPanel, chartPanel, chart, originalDataset, originalData);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(studentFrame, "Ошибка загрузки данных для группы: " + groupName, "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            });

            mainPanel.add(chartPanel, BorderLayout.CENTER);
            mainPanel.add(filterPanel, BorderLayout.SOUTH);
            mainPanel.add(namesScrollPane, BorderLayout.WEST);
            mainPanel.add(numberingPanel, BorderLayout.NORTH); // Добавляем панель с нумерацией

            studentFrame.add(mainPanel);
            studentFrame.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка загрузки данных для группы: " + groupName, "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }



    private static DefaultCategoryDataset createStudentDatasetWithNumbers(Map<String, Map<String, Double>> studentData) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int number = 1;

        for (Map.Entry<String, Map<String, Double>> entry : studentData.entrySet()) {
            String studentName = entry.getKey();
            Map<String, Double> grades = entry.getValue();

            // Используем номер студента для оси X
            dataset.addValue(grades.getOrDefault("ДЗ", 0.0), "ДЗ", Integer.toString(number));
            dataset.addValue(grades.getOrDefault("Упражнение", 0.0), "Упражнение", Integer.toString(number));
            number++;
        }

        return dataset;
    }

    private static void highlightStudentBar(JFreeChart chart, int studentNumber) {
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        // Сбрасываем цвета всех столбцов
        for (int i = 0; i < plot.getDataset().getColumnCount(); i++) {
            renderer.setSeriesPaint(0, Color.GRAY); // ДЗ
            renderer.setSeriesPaint(1, Color.GRAY); // Упражнение
        }

        String studentKey = Integer.toString(studentNumber);
        for (int i = 0; i < plot.getDataset().getRowCount(); i++) {

            renderer.setSeriesPaint(i, Color.GRAY);
        }
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesPaint(1, Color.RED);
    }


    private static Map<String, Map<String, Double>> sortStudentData(Map<String, Map<String, Double>> studentData, String key) {
        List<Map.Entry<String, Map<String, Double>>> entries = new ArrayList<>(studentData.entrySet());
        entries.sort((e1, e2) -> Double.compare(e2.getValue().getOrDefault(key, 0.0), e1.getValue().getOrDefault(key, 0.0)));

        Map<String, Map<String, Double>> sortedData = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : entries) {
            sortedData.put(entry.getKey(), entry.getValue());
        }
        return sortedData;
    }

    private static void updateChartDataset(JFreeChart chart, DefaultCategoryDataset newDataset) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setDataset(newDataset);
    }


    private static DefaultCategoryDataset createSortedDataset(Map<String, Map<String, Double>> data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Сортировка данных по сумме средних оценок за ДЗ и Упражнения для каждой группы
        List<Map.Entry<String, Map<String, Double>>> sortedGroups = new ArrayList<>(data.entrySet());

        sortedGroups.sort((entry1, entry2) -> {
            double averageDz1 = entry1.getValue().getOrDefault("ДЗ", 0.0);
            double averageExercise1 = entry1.getValue().getOrDefault("Упражнение", 0.0);
            double sum1 = averageDz1 + averageExercise1; // Сумма средних оценок за ДЗ и Упражнение для группы

            double averageDz2 = entry2.getValue().getOrDefault("ДЗ", 0.0);
            double averageExercise2 = entry2.getValue().getOrDefault("Упражнение", 0.0);
            double sum2 = averageDz2 + averageExercise2; // Сумма средних оценок за ДЗ и Упражнение для группы

            return Double.compare(sum2, sum1); // Сортировка по убыванию
        });

        // Заполнение отсортированных данных
        for (Map.Entry<String, Map<String, Double>> entry : sortedGroups) {
            String groupName = entry.getKey();
            Map<String, Double> topicAverages = entry.getValue();

            // Рассчитываем сумму среднего балла за ДЗ и Упражнение
            double averageDz = topicAverages.getOrDefault("ДЗ", 0.0);
            double averageExercise = topicAverages.getOrDefault("Упражнение", 0.0);
            double sum = averageDz + averageExercise;

            // Добавляем только одну колонку для суммы средних баллов за ДЗ и Упражнение
            dataset.addValue(sum, groupName, "Средний балл (ДЗ + Упражнение)");
        }

        return dataset;
    }
    private static void updateStudentList(JPanel namesPanel, Map<String, Map<String, Double>> studentData, JFreeChart chart) {
        namesPanel.removeAll();

        // Сортируем студентов по ДЗ или Упражнению (порядок уже задан при вызове)
        List<String> studentNames = new ArrayList<>(studentData.keySet());
        for (int i = 0; i < studentNames.size(); i++) {
            String studentName = studentNames.get(i);
            JButton studentButton = new JButton((i + 1) + ". " + studentName);

            // Добавляем обработчик для подсветки соответствующего столбца
            int studentNumber = i + 1;
            studentButton.addActionListener(e -> highlightStudentBar(chart, studentNumber));
            namesPanel.add(studentButton);
        }

        // Перерисовываем панель с именами
        namesPanel.revalidate();
        namesPanel.repaint();
    }

    private static void refreshUI(JPanel namesPanel, JPanel chartPanel, JFreeChart chart, DefaultCategoryDataset dataset, Map<String, Map<String, Double>> studentData) {
        // Обновляем график
        updateChartDataset(chart, dataset);

        // Обновляем список студентов
        updateStudentList(namesPanel, studentData, chart);

        // Перерисовываем панель с графиком
        chartPanel.removeAll();
        chartPanel.add(new ChartPanel(chart), BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

}