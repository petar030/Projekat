import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { ManagerUpdateComponent } from './manager-update-component';

describe('ManagerUpdateComponent', () => {
  let component: ManagerUpdateComponent;
  let fixture: ComponentFixture<ManagerUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManagerUpdateComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: '10' })
            }
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManagerUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
