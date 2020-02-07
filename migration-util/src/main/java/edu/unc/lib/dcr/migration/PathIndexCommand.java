/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dcr.migration;

import static edu.unc.lib.dcr.migration.MigrationConstants.OUTPUT_LOGGER;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dcr.migration.paths.PathIndexingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Commands for populating or pulling data from the path index
 *
 * @author bbpennel
 */
@Command(name = "path_index", aliases = {"pi"},
    description = "Interact with the index of paths for migration")
public class PathIndexCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private PathIndex pathIndex;

    @Option(names = {"-d", "--database-url"},
            defaultValue = "${sys:dcr.migration.index.url:-~/bxc_pindex",
            description = "Path where the database for the index is stored. Defaults to home dir.")
    private String databaseUrl;

    @Command(name = "populate",
            description = "Populate the index of file paths")
    public int populateIndex(
            @Parameters(index = "0", description = "Path to file listing FOXML documents.")
            Path objectListPath,
            @Parameters(index = "1", description = "Path to file listing datastream files.")
            Path dsListPath) {

        long start = System.currentTimeMillis();
        PathIndexingService service = getPathIndexingService();

        output.info(BannerUtility.getChompBanner("Populating path index"));

        output.info("Creating index at path {}", databaseUrl);
        service.createIndexTable();
        output.info("Populating object files from {}", objectListPath);
        service.indexObjects(objectListPath);
        output.info("Populating datastream files from {}", dsListPath);
        service.indexDatastreams(dsListPath);
        output.info("Finished populating index in {}ms", System.currentTimeMillis() - start);
        output.info("{} files were indexed", getPathIndex().countFiles());

        return 0;
    }

    @Command(name = "get_paths",
            description = "Get all paths for a uuid")
    public int getPaths(
            @Parameters(index = "0", description = "UUID of the object to get paths for")
            String uuid) {

        output.info("Paths for {}:", uuid);
        String paths = getPathIndex().getPaths(uuid).values().stream()
                .map(Path::toString)
                .collect(joining("\n"));
        output.info(paths);

        getPathIndex().close();

        return 0;
    }

    @Command(name = "delete",
            description = "Delete the index and all its files")
    public int deleteIndex() {
        output.info(BannerUtility.getChompBanner("Deleting path index"));

        getPathIndex().deleteIndex();
        return 0;
    }

    @Command(name = "count",
            description = "Count the files indexed")
    public int countFiles(
            @Option(names = {"-t"}, description = "Only count files of the specified type")
            String type) {

        PathIndex index = getPathIndex();

        try {
            if (type == null) {
                output.info("{} files are indexed", index.countFiles());
            } else {
                output.info("{} files of type {} are indexed", index.countFiles(Integer.parseInt(type)), type);
            }
        } finally {
            index.close();
        }

        return 0;
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    private PathIndex getPathIndex() {
        if (pathIndex == null) {
            pathIndex = new PathIndex();
            pathIndex.setDatabaseUrl(databaseUrl);
        }
        return pathIndex;
    }

    private PathIndexingService getPathIndexingService() {
        PathIndexingService service = new PathIndexingService();
        service.setPathIndex(getPathIndex());
        return service;
    }
}
