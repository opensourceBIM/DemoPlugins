package org.bimserver.demoplugins.service;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Formula;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class GeometryInfoToExcelPlugin extends AbstractAddExtendedDataService  {

	private static final String SCHEMA_NAME = SchemaName.INFO_3D_EXCEL_1_0.name();
	private WritableCellFormat times;
	private WritableCellFormat timesbold;

	public GeometryInfoToExcelPlugin() {
		super(SCHEMA_NAME);
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		super.newRevision(runningService, bimServerClientInterface, poid, roid, userToken, soid, settings);
	    WorkbookSettings wbSettings = new WorkbookSettings();
		
	    wbSettings.setLocale(new Locale("en", "EN"));
	    
	    WritableWorkbook workbook = null;

	    WritableFont times10pt = new WritableFont(WritableFont.ARIAL, 10);
	    times = new WritableCellFormat(times10pt);

	    WritableFont times10ptbold = new WritableFont(WritableFont.ARIAL, 10);
	    times10ptbold.setBoldStyle(WritableFont.BOLD);
	    timesbold = new WritableCellFormat(times10ptbold);
	    
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		workbook = Workbook.createWorkbook(byteArrayOutputStream, wbSettings);
	    WritableSheet sheet = workbook.createSheet("All", 0);
	    
	    sheet.addCell(new Label(0, 0, "Type", timesbold));
	    sheet.addCell(new Label(1, 0, "Guid", timesbold));
	    sheet.addCell(new Label(2, 0, "Triangles", timesbold));
	    sheet.addCell(new Label(3, 0, "Indices", timesbold));
	    sheet.addCell(new Label(4, 0, "Vertices", timesbold));
		sheet.addCell(new Label(5, 0, "Normals", timesbold));
		sheet.addCell(new Label(6, 0, "Colors", timesbold));
		sheet.addCell(new Label(7, 0, "Total triangle area", timesbold));
		sheet.addCell(new Label(8, 0, "Average triangle size", timesbold));
		
		int row = 2;
		
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);
		for (IfcProduct ifcProduct : model.getAllWithSubTypes(IfcProduct.class)) {
			if (ifcProduct.getGeometry() != null) {
				int nrTriangles = ifcProduct.getGeometry().getPrimitiveCount();
				GeometryData data = ifcProduct.getGeometry().getData();
				if (data != null) {
					int nrIndices = data.getNrIndices();
					int nrVertices = data.getNrVertices();
					int nrNormals = data.getNrNormals();
					int nrColors = data.getNrColors();
					
					TriangleIterator triangleIterator = new TriangleIterator(data);
					float totalArea =  0;
					while (triangleIterator.hasNext()) {
						Triangle triangle = triangleIterator.next();
						float area = triangle.area();
						totalArea += area;
					}
					
					sheet.addCell(new Label(0, row, ifcProduct.eClass().getName(), times));
					sheet.addCell(new Label(1, row, ifcProduct.getGlobalId(), times));
					sheet.addCell(new jxl.write.Number(2, row, nrTriangles, times));
					sheet.addCell(new jxl.write.Number(3, row, nrIndices, times));
					sheet.addCell(new jxl.write.Number(4, row, nrVertices, times));
					sheet.addCell(new jxl.write.Number(5, row, nrNormals, times));
					sheet.addCell(new jxl.write.Number(6, row, nrColors, times));
					sheet.addCell(new jxl.write.Number(7, row, totalArea, times));
					sheet.addCell(new Formula(8, row, "H" + (row + 1) + "/C" + (row + 1), times));
					
					row++;
				}
			}
		}
	    
	    workbook.write();
		workbook.close();

		byte[] bytes = byteArrayOutputStream.toByteArray();
		addExtendedData(bytes, "geometryinfo.xls", "Excel LOD Results", "application/excel", bimServerClientInterface, roid);
	}
}
