import service.PaperService;

public class Test01 {
    public static void main(String[] args) {
        PaperService paperService = new PaperService();
//        String filePath = "/Users/develop/LocalPaperIndexer/src/test/resources/testPDFs/spotfi.pdf";
        String filePath = "/Users/develop/LocalPaperIndexer/src/test/resources/testPDFs";
        String content = paperService.readPdfContent(filePath);
        System.out.println(content.length());
    }
}
