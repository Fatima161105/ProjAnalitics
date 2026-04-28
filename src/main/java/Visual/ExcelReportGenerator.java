package Visual;

import Database.DatabaseService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ExcelReportGenerator {

    private final DatabaseService databaseService;

    public ExcelReportGenerator(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void generateExcelReport() throws SQLException, IOException {
        // Получаем данные для отчета
        Map<String, Map<String, Double>> groupGrades = databaseService.getGradesByGroupAndTopic();

        // Создаем новый Excel файл
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Отчет");

        // Добавляем заголовки
        Row gradeHeaderRow = sheet.createRow(0);
        gradeHeaderRow.createCell(0).setCellValue("Группа");
        gradeHeaderRow.createCell(1).setCellValue("Тема");
        gradeHeaderRow.createCell(2).setCellValue("Средний балл");

        // Заполняем таблицу оценками
        int rowIndex = 1;
        for (Map.Entry<String, Map<String, Double>> groupEntry : groupGrades.entrySet()) {
            String groupName = groupEntry.getKey();
            Map<String, Double> topics = groupEntry.getValue();

            for (Map.Entry<String, Double> topicEntry : topics.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(groupName);
                row.createCell(1).setCellValue(topicEntry.getKey());
                row.createCell(2).setCellValue(topicEntry.getValue());
            }
        }

        // Создание столбчатой диаграммы
        createBarChartAsImage(sheet, rowIndex, workbook);

        // Сохранение Excel файла
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить отчет");
        fileChooser.setSelectedFile(new File("Отчет.xlsx"));
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileOutputStream fileOut = new FileOutputStream(fileToSave)) {
                workbook.write(fileOut);
                JOptionPane.showMessageDialog(null, "Отчет успешно сохранен.", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Ошибка при сохранении файла.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void createBarChartAsImage(Sheet sheet, int rowIndex, XSSFWorkbook workbook) {
        // Списки для данных
        List<String> groupNames = new ArrayList<>();
        List<Double> averageGrades = new ArrayList<>();

        // Заполнение списков данными из Excel
        for (int i = 1; i < rowIndex; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String groupName = row.getCell(0) != null ? row.getCell(0).getStringCellValue() : "Без имени группы";
            Cell averageGradeCell = row.getCell(2);

            if (averageGradeCell != null && averageGradeCell.getCellType() == CellType.NUMERIC) {
                groupNames.add(groupName);
                averageGrades.add(averageGradeCell.getNumericCellValue());
            }
        }

        if (groupNames.isEmpty() || averageGrades.isEmpty()) {
            System.out.println("Нет данных для создания диаграммы.");
            return;
        }

        // Создаем набор данных для JFreeChart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < groupNames.size(); i++) {
            dataset.addValue(averageGrades.get(i), "Средний балл", groupNames.get(i));
        }

        // Создаем диаграмму
        JFreeChart barChart = ChartFactory.createBarChart(
                "Средний балл по группам",
                "Группы",
                "Средний балл",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false);

        // Сохраняем диаграмму в изображение
        try {
            int width = 940;   // Ширина изображения
            int height = 480;  // Высота изображения
            BufferedImage chartImage = barChart.createBufferedImage(width, height);

            ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
            ImageIO.write(chartImage, "png", chartOut);
            chartOut.close();

            // Вставляем изображение в Excel
            int pictureIdx = workbook.addPicture(chartOut.toByteArray(), Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper helper = workbook.getCreationHelper();

            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(5); // Начальная колонка
            anchor.setRow1(1); // Начальная строка
            drawing.createPicture(anchor, pictureIdx);

            System.out.println("Диаграмма добавлена как изображение.");
        } catch (IOException e) {
            System.out.println("Ошибка при создании изображения диаграммы: " + e.getMessage());
        }
    }
}

