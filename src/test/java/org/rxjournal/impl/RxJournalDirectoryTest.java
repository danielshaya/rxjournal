package org.rxjournal.impl;

import io.reactivex.Flowable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RxJournalDirectoryTest {

    Path base;
    Path expectedDir;

    @Before
    public void createDir() throws IOException {
        base = Files.createTempDirectory("RxJournal");
        expectedDir = base.resolve(RxJournal.RXJOURNAL_DIRNAME);
    }

    @After
    public void deleteDir() throws IOException {
        if (Files.exists(expectedDir)) {
            Files.walk(expectedDir)
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && (
                            f.getName().endsWith(".txt") || f.getName().endsWith(".cq4")))
                    .peek(f -> System.out.println("will delete " + f))
                    .forEach(File::delete);
            Files.deleteIfExists(expectedDir);
        }
        Files.deleteIfExists(base);
    }

    @Test
    public void rxJournalUsesDefaultSubDir() throws IOException {
        RxJournal journal = new RxJournal(base.toString());

        Assert.assertEquals(expectedDir.toString(), journal.getDir());
    }

    @Test
    public void rxJournalUsesSpecificSubDirWhenNamed() throws IOException {
        RxJournal journal = new RxJournal(base.toString(), "foo");

        Assert.assertNotEquals(expectedDir.toString(), journal.getDir());
        Assert.assertEquals(expectedDir.toString().replace(RxJournal.RXJOURNAL_DIRNAME, "foo"), journal.getDir());
    }

    @Test
    public void rxJournalDoesntCreateDir() {
        RxJournal journal = new RxJournal(base.toString());

        Assert.assertFalse(Files.exists(expectedDir));
    }

    @Test
    public void rxJournalCreatesDirOnRecording() throws IOException {
        RxJournal journal = new RxJournal(base.toString());
        RxRecorder recorder = journal.createRxRecorder();

        Assert.assertFalse(Files.exists(expectedDir));

        recorder.record(Flowable.just("foo"), "");
        Assert.assertTrue(Files.exists(expectedDir));
    }

    @Test
    public void rxJournalClearsIfOnlyCq4Files() throws IOException {
        RxJournal journal = new RxJournal(base.toString());

        RxRecorder recorder = journal.createRxRecorder();
        recorder.record(Flowable.just("foo"), "");
        Assert.assertTrue(Files.exists(expectedDir));

        //short circuit the test if the dir is unexpected, just in case the clearCache would then be too destructive
        Assert.assertEquals(expectedDir.toString(), journal.getDir());

        journal.clearCache();

        Assert.assertFalse(Files.exists(expectedDir));
    }

    @Test
    public void rxJournalClearsOnlyCq4FilesAndKeepsBaseDir() throws IOException {
        Files.createDirectories(expectedDir);
        Path tempFile = Files.createTempFile(expectedDir, "rxJournalClearsOnlyCq4", ".txt");

        RxJournal journal = new RxJournal(base.toString());

        RxRecorder recorder = journal.createRxRecorder();
        recorder.record(Flowable.just("foo"), "");

        //short circuit the test if the dir is unexpected, just in case the clearCache would then be too destructive
        Assert.assertEquals(expectedDir.toString(), journal.getDir());

        journal.clearCache();

        Assert.assertTrue(Files.exists(expectedDir));
        Assert.assertTrue(Files.exists(tempFile));

        long count = Files.walk(expectedDir, 1)
                .map(Path::toFile)
                .filter(File::isFile)
                .count();

        Assert.assertEquals(1, count);
    }

}
